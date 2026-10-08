# design.md — F12-estimated-hours

## Decisões
- **Valor + unidade no mesmo record, não um número com unidade fixa.** O usuário pediu
  unidade escolhível "como a tarefa principal" (horas **ou** dias). Modelamos `estimatedTime`
  + `estimatedUnit` (enum `HOURS | DAYS`) em vez de converter tudo para horas, para exibir o
  número que o dono digitou.
- **Ambos nulos juntos = sem tempo.** Migration com `estimated_time DOUBLE PRECISION` e
  `estimated_unit VARCHAR(10)`, nulas; regra de escrita garante coesão: valor sem unidade usa
  `HOURS` (default), unidade sem valor é descartada. Não há `CHECK` no banco — a regra vive
  no domínio (uma coluna com `HOURS` e outra com `DAYS` não acontece por construção) e a
  validação de faixa (`> 0`, `<= 200`) nos request DTOs com `@DecimalMin/@DecimalMax`.
- **Teto de 200** (unidade implícita: horas ou dias) com mensagem pt-BR no campo; o `form`
  valida antes de enviar e completa o contrato com os mesmos limites.
- **Camadas sem vazamento:** `TaskCommand` (entrada do serviço) ganha `tempoEstimado`/
  `unidadeTempo`; `TaskService.toTask/update` mapeiam para a entidade; `TaskMapper` serializa
  `estimatedTime/estimatedUnit`; construtor de 5 args de `Task` preservado como overload
  (delega com `null, null`) para não quebrar o histórico de teste.
- **IA não decide unidade:** a decomposição grava o `estimatedHours` que o modelo já devolve
  como `HOURS`; "melhorar" reenvia no PUT o tempo atual da tarefa (senão o backend zeraria o
  campo porque o body não o traz). `AiTaskController.comandoDe(SubtaskDraft)` deixou de ter o
  javadoc "sem coluna".
- **Expansão no frontend por componente dedicado, não estado global do dashboard.**
  `PrazoLinha` encapsula aberto/carregando/filhos/erro por linha; o clique busca
  `GET /tasks/{id}/subtasks` uma única vez (cache em memória da linha) e renderiza um
  `<ul class="dashboard__prazos dashboard__prazos--nivel">` recuado, recursivo para níveis
  seguintes. O dashboard não recarrega a lista de prazos ao expandir.
- **Formatação de tempo em um util:** `formatarTempoEstimado(2, 'HOURS') → "2 horas"`,
  `1 → "1 hora"`, `DAYS → "dia/dias"`; vazio quando falta valor ou unidade. Reutilizada no
  detalhe e nos prazos.

## Fluxo (backend, escrita)
```
CreateTaskRequest/UpdateTaskRequest {estimatedTime, estimatedUnit}
   → valida @DecimalMin(0,false) @DecimalMax(200) (400 ProblemDetail pt-BR)
   → TaskCommand {tempoEstimado, unidadeTempo}
   → TaskService
        toTask: Task.setEstimatedTime(v, u)   → null/null quando ausentes
        update: request.getEstimatedTime/getEstimatedUnit → entidade
   → TaskResponse {estimatedTime, estimatedUnit}
```

## Fluxo (dashboard, leitura)
```
GET /tasks?size=50 → raízes (listar prazos)
PrazoLinha (por linha com subtaskCount > 0 mostra toggle ▸)
   clique → GET /tasks/{id}/subtasks → filhos ordenados
          → <ul .dashboard__prazos--nivel> (recuo 1.25rem, borda esquerda)
              → PrazoLinha(filho) recursivo (mesmo toggle se tiver subtarefas)
   cada linha: título + [tempo formatado] + data (Atrasada · dd/mm/aaaa)
```

## Backend
- **Migration:** `V6__tasks_estimated_time.sql` (ADD COLUMN `estimated_time`, `estimated_unit`).
- **`task/domain/TimeUnit.java`:** enum `HOURS | DAYS`.
- **`task/domain/Task.java`:** campos + getters; `setEstimatedTime(Double, TimeUnit)` com a
  regra de negócio (`BusinessRuleException` para `<= 0`/`> 200`); `updateContent` variante
  com os 2 campos; construtor 5 args mantido (delega `null, null`).
- **`task/api/dto/*`:** `TaskResponse` (getters, construtor), `CreateTaskRequest`/
  `UpdateTaskRequest` (campos + `@DecimalMin("0", inclusive=false)`/`@DecimalMax("200")`
  com mensagens pt-BR).
- **`task/application/dto/TaskCommand.java`:** + `tempoEstimado`/`unidadeTempo` (6 campos) e
  construtor de conveniência de 4 args para os callers que não usam tempo.
- **`TaskService`/`TaskMapper`/`TaskController`:** repasse de `estimatedTime/estimatedUnit`.
- **`ai/api/AiTaskController.java`:** `comandoDe(SubtaskDraft)` grava `estimatedHours` +
  `TimeUnit.HOURS`; import de `TimeUnit`.
- **Testes:** `TaskTest` (nested `TempoEstimado`: define, default HOURS, valida, unidade sem
  valor descartada, limite), `TaskServiceTest` (grava e serializa), `TaskControllerTest`
  (repasse + 400 para 0 e para 201), `TaskMapperTest` (cobertura do tempo), `AiTaskControllerTest`
  (comando com HOURS e null). Suite: 272 testes.

## Frontend
- **`types/task.ts`:** `TaskTimeUnit`, `estimatedTime/estimatedUnit` em `Task`/`TaskInput`,
  `TIME_UNIT_LABELS` (Horas/Dias), `TIME_UNIT_OPTIONS`.
- **`utils/tempo.ts`:** `formatarTempoEstimado` (singular/plural, vazio se faltar dado).
- **`TaskForm.tsx`:** campos "Tempo estimado" + "Unidade" (linha `.task-form__linha`, flex
  wrap); validação "maior que zero e até 200" antes do submit; pré-preenche na edição;
  envia `null`/`null` quando o tempo fica em branco (apaga no backend).
- **`TaskDetail.tsx`:** meta ganha "Tempo estimado".
- **`AiPanel.tsx`:** `aplicarMelhoria` reenvia `estimatedTime/estimatedUnit` atuais no PUT.
- **`components/dashboard/PrazoLinha.tsx`:** linha expansível (toggle `▸/▾` com
  `aria-expanded`/`aria-label`), estados carregando/vazio/erro, lista recuada recursiva.
- **`index.css`:** `.dashboard__prazo`, `.dashboard__prazo-linha` (flex space-between),
  `.dashboard__prazo-toggle`, `.dashboard__prazo-tempo`, `.dashboard__prazos--nivel`
  (recuo + borda esquerda).
- **Testes:** `TaskForm.test` (envio com unidade, inválido, prefill), `TaskDetail.test`
  (tempo formatado/ausente), `tempo.test` (utils), `DashboardPage.test` (expandir: subtarefa
  recuada + tempo + recolher; toggle ausente sem subtarefas). Suite: 59 testes.

## Evoluções futuras (não implementadas)
- Somar tempos de subtarefas e mostrar total estimado na raiz; registo de horas gastas.
- Configuração de unidade global (todas em horas ou todas em dias).
- Exibir tempo nos cards de prioridade/prazos do assistente.