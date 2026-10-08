# design.md — F13-prazo-tempo-ia

## Contrato de API

### Nova rota
```
POST /api/ai/tasks/{id}/analysis/apply
Body: { "priority": "HIGH", "estimatedHours": 12.0 }   // estimatedHours opcional
200 → TaskResponse (com estimatedTime/estimatedUnit já aplicados)
422 → BusinessRuleException (id inexistente vira 404)
```

- `ApplyAnalysisRequest(TaskPriority priority, @DecimalMin(0,false) @DecimalMax(200) Double estimatedHours)`.
- `estimatedHours` é o valor que a própria análise (`POST /{id}/analyze`) sugeriu; o servidor
  recalcula no ato: raiz com subtarefas usa a **soma em horas** das filhas (Dias → ×24);
  raiz sem subtarefas e subtarefa usam o `estimatedHours` do body.
- A escrita do tempo na raiz fica **fora** da regra de R1 (que vale só para `POST /tasks` e
  `PUT /tasks/{id}`): é um caminho de escrita explícito da IA, documentado.

## Backend

- **`Task`:** novo método `applySuggestion(TaskPriority prioridade, Double tempoHoras)` que
  troca a prioridade e grava tempo (unidade `HOURS`) via o mesmo `setEstimatedTime` já testado.
- **`TaskService`:**
  - `create`: lança `BusinessRuleException` ("tarefa principal nao pode ter tempo estimado")
    quando o comando traz `tempoEstimado != null`.
  - `createSubtask`: lança "subtarefa nao pode ter prazo" quando o comando traz `prazo != null`.
  - `update`: aplica a regra conforme `task.isSubtask()`.
  - `applySuggestion(UUID id, TaskPriority prioridade, Double horasLlms)`: resolve raiz × subtarefa
    (id, subtarefas, soma) e delega ao `Task.applySuggestion`.
- **`AiTaskService`:** caso de uso da F03 expõe `applySuggestion(UUID id, TaskPriority prioridade,
  Double horasLlms)` delegando ao `TaskService` (transacional). A soma das filhas usa `findSubtasks`.

### Cálculo da soma
`sum(filha.estimatedTime * (filha.estimatedUnit == DAYS ? 24 : 1))`. Só aplica quando a soma > 0;
senão cai para `estimatedHours` do body; se este for nulo, o tempo da raiz não muda.

## Frontend

- **`TaskForm`:** prop nova `eSubtarefa: boolean` (derivada de `task?.parentId` na `TasksPage`).
  Se raiz → renderiza só o campo **Prazo**; se subtarefa → só **Tempo estimado + Unidade**.
  O submit envia `null` no campo oculto.
- **`TaskDetail`:** remove o estado/efeito do `pai` e o bloco "Subtarefa de X"; a meta vira
  Prioridade, Tempo estimado (`formatarTempoEstimado`), Criada em (`formatarDataBR`), Prazo
  (`formatarDataBR`) — linhas de Tempo e Prazo condicionais (subtask × root).
- **`TasksPage`:** busca o pai quando `selecionada.parentId` e guarda `paiDaSelecionada`; o
  breadcrumb e o título da página usam `Tarefas / <pai> / <subtarefa>`.
- **`AiPanel`:** `AnalysisResult` ganha `onAplicar` e o botão **"Aplicar Sugestão"** junto de
  "Fechar"; `AiPanel` implementa com `applyAnalysis(id, { priority, estimatedHours })` e chama
  `onChanged`. `operacao` nova `'aplicar-analise'`.
- **`PrazoLinha`:** linha usa `tarefa.parentId` para decidir raiz × subtarefa — raiz renderiza
  data (+ "Atrasada"), subtarefa renderiza `formatarTempoEstimado` + " para realizar". O
  `<strong>` vira botão com o mesmo `alternar()` da setinha.
- **`.ai-panel`:** `padding-left: var(--space-3)` para afastar os botões da borda de design.

## Arquivos
- Backend: `task/domain/Task.java`, `task/application/TaskService.java`,
  `task/application/AiTaskService.java`, `ai/api/AiTaskController.java`,
  `ai/api/dto/ApplyAnalysisRequest.java` (novo), testes.
- Frontend: `components/task/{TaskForm,TaskDetail,AiPanel,AnalysisResult}.tsx`,
  `pages/TasksPage.tsx`, `components/dashboard/PrazoLinha.tsx`, `api/ai.ts`,
  `utils/date.ts` (sem mudança — uso padrão), `index.css`, testes.