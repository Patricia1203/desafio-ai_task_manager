# spec.md — F13-prazo-tempo-ia

> Feature criada em 2026-10-08 a pedido do usuário: separar a responsabilidade da tarefa —
> **tarefa principal só carrega prazo, subtarefa só carrega tempo estipulado** —, dar
> espaçamento ao bloco de ações da IA, permitir **clicar no título da raiz** do dashboard
> para revelar as subtarefas, padronizar data em **dd/mm/aaaa** e deixar a **análise da IA**
> ser aplicada de um só clique ("Aplicar Sugestão").

## Objetivo
As raízes (tarefas criadas pelo formulário) passam a viver de **prazo** (data de entrega); as
subtarefas (geradas pela IA) passam a viver de **tempo estipulado**. O formulário, o detalhe,
os "Próximos prazos" do dashboard e a aplicação da análise respeitam essa divisão; a navegação
para uma subtarefa mantém o pai no breadcrumb; e a análise da IA ganha um botão que aplica a
sugestão na própria tarefa.

## Requisitos

### R1 — Regra raiz/prazo, subtarefa/tempo (API e tela)
- **Backend:** `TaskService.create` rejeita `estimatedTime` em tarefa **raiz** (sem pai) com
  `BusinessRuleException` (422); `TaskService.createSubtask` rejeita `dueDate` em **subtarefa**
  (tem pai); `TaskService.update` aplica a mesma regra conforme a tarefa atual seja raiz ou
  subtarefa. O contrato (`CreateTaskRequest`/`UpdateTaskRequest`) continua aceitando os campos —
  a regra é de negócio, não de validação de borda. `TaskMapper` continua serializando tempo e
  prazo (análise pode atribuir tempo a uma raiz; a regra vale para escrita manual via API de
  tarefas).
- **Frontend (form):** `TaskForm` mostra **Prazo** para raiz e **Tempo estimado + Unidade**
  para subtarefa (um ou outro, nunca os dois).
- **Frontend (detalhe):** a meta do `TaskDetail` passa a ser **Prioridade, Tempo estimado,
  Criada em, Prazo** (o prazo fica depois de "Criada em"); na raiz o prazo aparece e a linha de
  tempo fica oculta; na subtarefa aparece o tempo e o prazo fica oculto. Remover o selo
  "Subtarefa de <pai>" de dentro do detalhe: a origem passa a ser só o breadcrumb.

### R2 — Breadcrumb com o pai na frente
- Em uma subtarefa: `Dashboard / Tarefas / <título da principal> / <título da subtarefa>`
  (o caminho da principal **não** é substituído — a subtarefa entra na frente). A tela busca o
  pai (`GET /tasks/{id}`) quando a selecionada tem `parentId`.

### R3 — "Próximos prazos" do dashboard
- Na **raiz** a linha mostra a **data** (prazo), com "Atrasada" quando aplicável; na
  **subtarefa** mostra o **tempo estimado** (ex.: "2 horas para realizar") e nunca a data.
- **Clicar no título** da raiz também expande/recolhe as subtarefas (não só a setinha).

### R4 — Espaçamento das ações da IA
- `.ai-panel` ganha respiro à esquerda (padding) para os botões **Melhorar / Analisar /
  Dividir** não colarem na linha de design.

### R5 — Aplicar Sugestão da análise
- Endpoint novo `POST /ai/tasks/{id}/analysis/apply` com corpo `{ priority, estimatedHours }`:
  aplica a **prioridade** sugerida; o **tempo estimado** da raiz é a **soma em horas das
  subtarefas** quando existem (senão usa o `estimatedHours` da própria análise), e na subtarefa
  usa o `estimatedHours` direto. Resposta: `TaskResponse` (200). A raiz pode ganhar
  `estimatedTime` por este caminho (regra R1 vale para a escrita manual).
- **Frontend:** `AnalysisResult` ganha o botão **"Aplicar Sugestão"** (chamada ao endpoint,
  atualiza a tarefa em tela). "Complexidade" da análise segue sem campo para persistir (decisão
  R5) — o botão aplica prioridade + tempo.

### R6 — Data sempre dd/mm/aaaa
- Toda exibição de data usa `formatarDataBR` (inclusive **Prazo** e **Criada em** no detalhe).

## Fora de escopo (decisões)
- Sem somatório automático persistido no banco: a soma é calculada na hora do "Aplicar Sugestão"
  e guardada como `estimatedTime` da raiz (unidade `HOURS`), não como campo novo.
- A "complexidade" da análise continua descartável (não há coluna); aplicar sugestão ignora.
- Áreas de trabalho (agrupamento estilo Trello) **não** fazem parte desta feature.

## Sucesso
1. POST/PUT de raiz com `estimatedTime` → 422; criar subtarefa (decompose/apply) com `dueDate` →
   422; subtarefa com tempo grava e serializa.
2. Form de raiz mostra só Prazo; form de subtarefa mostra só Tempo/Unidade; detalhe mostra a
   meta na nova ordem, com o prazo depois de "Criada em", e sem o selo "Subtarefa de X".
3. Breadcrumb de subtarefa: `Dashboard / Tarefas / <pai> / <subtarefa>`.
4. Dashboard: raiz mostra data, subtarefa mostra "X horas para realizar"; clicar no título
   expande.
5. Análise → "Aplicar Sugestão" atualiza prioridade e o tempo (soma das subs ou horas da IA).
6. Data exibida sempre dd/mm/aaaa.
7. Gates: `mvn test`, `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build` verdes.