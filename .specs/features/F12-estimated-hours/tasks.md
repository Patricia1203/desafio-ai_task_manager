# tasks.md — F12-estimated-hours

> Feature criada em 2026-10-08 a pedido do usuário: tempo estimado por tarefa/subtarefa com
> unidade escolhível (horas ou dias, "como a tarefa principal") e expansão do bloco
> "Próximos prazos" do dashboard com subtarefas recuadas e tempo de cada.

### T-F12-01 — Backend: persistir tempo estimado com unidade (horas ou dias)
- **Status:** done (commit `a7aa054`, 2026-10-08)
- **Reqs:** RF-25
- **Depends on:** —
- **Arquivos (alterar):** `db/migration/V6__tasks_estimated_time.sql`,
  `task/domain/{Task,TimeUnit}.java`, `task/api/{TaskMapper,TaskController}.java`,
  `task/api/dto/{TaskResponse,CreateTaskRequest,UpdateTaskRequest}.java`,
  `task/application/{TaskService}.java`, `task/application/dto/TaskCommand.java`,
  `ai/api/AiTaskController.java`, testes (`TaskTest`, `TaskServiceTest`,
  `TaskControllerTest`, `TaskMapperTest`, `AiTaskControllerTest`)
- **O que fazer:** colunas `estimated_time`/`estimated_unit` (ambas nulas juntas); enum
  `TimeUnit`; `Task.setEstimatedTime` com regra `> 0` e `<= 200` e default `HOURS`;
  contrato EN com `estimatedTime/estimatedUnit`; `TaskCommand` 6 campos; decomposição grava
  `estimatedHours` como `HOURS`.
- **Pronto quando:** criar/editar grava e serializa; `0`/`201` retornam `400` apontando o
  campo; confirmado via API real (PUT + GET).
- **Gate:** `mvn test` verde (272).

### T-F12-02 — Frontend: form e detalhe gravam/exibem o tempo estimado
- **Status:** done (commit `0026e6b`, 2026-10-08)
- **Reqs:** RF-25
- **Depends on:** T-F12-01
- **Arquivos (alterar):** `frontend/src/types/task.ts`, `utils/tempo.ts`,
  `components/task/{TaskForm,TaskDetail,AiPanel}.tsx`, `test/{TaskForm,TaskDetail,AiPanel,TaskList,tempo}.test.tsx`
- **O que fazer:** `TaskTimeUnit` + campos em `Task`/`TaskInput`; campos "Tempo estimado" +
  "Unidade" no form (validação e prefill); meta do detalhe; `aplicarMelhoria` preserva o
  tempo no PUT; `formatarTempoEstimado` com singular/plural.
- **Pronto quando:** form envia valor+unidade e apaga com branco; detalhe mostra "2 horas"/
  "Sem tempo estimado"; melhoria não zera; suite verde.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run` (58), `npx vite build` verdes.

### T-F12-03 — Dashboard: "Próximos prazos" expansível com subtarefas e tempo
- **Status:** done (commits `84b52c1`, `5569f70`, `9b7e27b`, 2026-10-08)
- **Reqs:** RF-25
- **Depends on:** T-F12-02
- **Arquivos (alterar):** `frontend/src/components/dashboard/PrazoLinha.tsx` (novo),
  `pages/DashboardPage.tsx`, `pages/DashboardPage.test.tsx`, `index.css`
- **O que fazer:** `PrazoLinha` com toggle quando `subtaskCount > 0`; clique busca
  `getSubtasks` e lista os filhos recuados (`dashboard__prazos--nivel`), recursivo, com o
  tempo de cada linha; estados carregando/vazio/erro.
- **Retoques da revisão:** título da raiz alinhado à esquerda (saiu do `space-between` —
  `5569f70`); rolagem vertical **só quando passa de 3 subtarefas** (classe condicional
  `dashboard__prazos--rolagem` com `max-height 9rem` + `overflow-y auto` — `9b7e27b`).
- **Pronto quando:** expandir mostra subtarefas recuadas com tempo, recolher volta; sem
  recarga; verificado no dev server com dados reais; com 3 ou menos o bloco não rola.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run` (61), `npx vite build` verdes;
  E2E headless.

### T-F12-04 — Docs e rastreabilidade
- **Status:** done (2026-10-08)
- **Reqs:** DOC-01
- **Depends on:** T-F12-02, T-F12-03
- **Arquivos (alterar):** `.specs/features/F12-estimated-hours/{spec,design,tasks}.md`,
  `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`
- **O que fazer:** spec/design/tasks da F12; RF-25 na matriz com os SHAs
  (`a7aa054`, `0026e6b`, `84b52c1`, `5569f70`, `9b7e27b`); STATE com o resumo e as decisões
  (inclui os fixes de dev-env/UX desta rodada, já commitados: `6c3c094`, `33e7a80`, `9668e58`).
- **Pronto quando:** matriz e STATE refletem o recurso; sem pendências não justificadas.
- **Gate:** `grep -c "done (parcial"` continua 0; `mvn test` (272), `npx vitest run` (61)
  verdes.