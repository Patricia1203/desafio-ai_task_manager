# tasks.md — F13-prazo-tempo-ia

> Feature criada em 2026-10-08 a pedido do usuário (rodada de ajustes pós-F12): separar
> prazo × tempo entre raiz e subtarefa, dar respiro às ações da IA, expandir o dashboard também
> pelo título, data sempre dd/mm/aaaa, breadcrumb mantendo o pai e botão de aplicar a análise.

### T-F13-01 — Backend: regra raiz-só-prazo / subtarefa-só-tempo e aplicar a sugestão
- **Status:** done
- **Reqs:** RF-12, RF-25 (ajuste)
- **Depends on:** —
- **Arquivos (alterar):** `task/domain/Task.java`, `task/application/{TaskService,AiTaskService}.java`,
  `ai/api/AiTaskController.java`, `ai/api/dto/ApplyAnalysisRequest.java` (novo), testes
  (`TaskServiceTest`, `AiTaskServiceTest`, `AiTaskControllerTest`).
- **O que fazer:** (1) `create` (raiz) recusa tempo estimado, `createSubtask` (subtarefa)
  recusa prazo, `update` aplica conforme a tarefa atual — 422 com mensagem pt-BR; (2)
  `Task.applySuggestion` (prioridade + tempo HOURS, horas nulas só trocam prioridade);
  `TaskService.applySuggestion` soma as filhas em horas (Dias ×24) quando existem, senão usa
  `estimatedHours` da análise; na subtarefa grava o estimado direto; endpoint
  `POST /ai/tasks/{id}/analysis/apply` `{priority, estimatedHours}` → 200 TaskResponse.
- **Pronto quando:** raiz com tempo → 422; subtarefa com prazo → 422; raiz com filhas aplica
  prioridade + soma em horas; raiz sem filhas usa as horas da análise; subtarefa grava direto;
  id inexistente → 404; teto 200 e `priority` obrigatória validados; suítes verdes.
- **Gate:** `mvn test` (285).

### T-F13-02 — Frontend: form condicional, detalhe reordenado, breadcrumb e data (done)
- **Status:** done
- **Reqs:** RF-25 (ajuste)
- **Depends on:** T-F13-01
- **Arquivos (alterar):** `components/task/{TaskForm,TaskDetail}.tsx`, `pages/TasksPage.tsx`,
  testes (`TaskForm.test`, `TaskDetail.test`, cobertura de TasksPage).
- **O que fazer:** form mostra Prazo (raiz) ou Tempo/Unidade (subtarefa); meta do detalhe na
  ordem **Prioridade, Tempo estimado, Criada em, Prazo**, prazo sempre `formatarDataBR`, linhas
  condicionais (raiz prazo × subtarefa tempo); remover o selo "Subtarefa de X"; breadcrumb da
  subtarefa `Tarefas / <pai> / <subtarefa>`.
- **Pronto quando:** raiz não mostra tempo no form; subtarefa não mostra prazo; meta na nova
  ordem com data dd/mm/aaaa; breadcrumb traz o pai; testes verdes.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build`.

### T-F13-03 — Frontend: dashboard (data × tempo, clique no título) e respiro da IA (done)
- **Status:** done
- **Reqs:** RF-25 (ajuste)
- **Depends on:** T-F13-01 (visual, sem dependência do endpoint)
- **Arquivos (alterar):** `components/dashboard/PrazoLinha.tsx`, `index.css`, testes
  (`DashboardPage.test`).
- **O que fazer:** raiz mostra data (com "Atrasada"); subtarefa mostra `tempo + " para
  realizar"`; clicar no título expande/recolhe; `.ai-panel` ganha `padding-left`.
- **Pronto quando:** linhas raiz × subtarefa mostram o campo certo; título expande; botões da
  IA não colam na borda; testes verdes.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build`.

### T-F13-04 — Frontend: "Aplicar Sugestão" na análise (done)
- **Status:** done
- **Reqs:** RF-12, RF-25 (ajuste)
- **Depends on:** T-F13-01
- **Arquivos (alterar):** `components/task/{AiPanel,AnalysisResult}.tsx`, `api/ai.ts`,
  `types/ai.ts`, testes (`AiPanel.test`).
- **O que fazer:** `applyAnalysis(id, {priority, estimatedHours})`; `AnalysisResult` com botão
  "Aplicar Sugestão" + "Fechar"; `AiPanel` chama e `onChanged` atualiza a tarefa; erro tratado.
- **Pronto quando:** aplicar grava prioridade+tempo e atualiza a tela; testes verdes.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build`.

### T-F13-05 — Docs e rastreabilidade (done)
- **Status:** done
- **Reqs:** DOC-01
- **Depends on:** T-F13-01..04
- **Arquivos (alterar):** `.specs/features/F13-prazo-tempo-ia/{spec,design,tasks}.md`,
  `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`
- **Pronto quando:** matriz e STATE refletem a F13; SHAs por task.
- **Gate:** `grep -c "done (parcial"` matriz = 0; suítes verdes.