# tasks.md — F09-subtask-completion

> Feature criada em 2026-10-08 a pedido do usuário. O usuário respondeu as duas perguntas de
> design em 2026-10-08: (a) contrato = campo opcional `completeSubtasks` no `PATCH status`;
> (b) recusar o modal = cancela tudo. Implementação autorizada (fluxo em `design.md`/`spec.md`).

### T-F09-01 — Backend: campo `completeSubtasks` no PATCH status e conclusão das filhas
- **Status:** done (commit `1acbd1c`, 2026-10-08)
- **Reqs:** RF-06, RF-14
- **Depends on:** —
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/task/api/dto/UpdateStatusRequest.java`, `backend/src/main/java/com/desafio/taskmanager/task/application/TaskService.java`, `backend/src/main/java/com/desafio/taskmanager/task/api/TaskController.java`, testes (`TaskServiceTest`, `TaskControllerTest`, `TaskMapperTest`)
- **O que fazer:** `UpdateStatusRequest` ganha `Boolean completeSubtasks` opcional (contrato EN, F06). `TaskService.changeStatus` ganha variante de 3 argumentos (`id, status, completeSubtasks`) que, com `completeSubtasks == true` e `status == DONE`, conclui o pai e todas as subtarefas diretas não-DONE (`findByParentIdOrderByCreatedAtAsc`) na mesma transação; o método de 2 argumentos permanece delegando com `false` (retrocompatível). Controller repassa `Boolean.TRUE.equals(request.completeSubtasks())`. O campo é ignorado quando o status não é `DONE`.
- **Pronto quando:** `changeStatus(id, DONE, true)` conclui pai e filhas pendentes; `changeStatus(id, DONE, false)` e PATCH sem o campo mantêm o comportamento atual (só o pai); flag com status ≠ DONE não conclui filhas; testes do service, controller e mapper verdes.
- **Gate:** `mvn test` verde (suíte completa).

### T-F09-02 — Frontend: modal de confirmação ao concluir pai com subtarefas pendentes
- **Status:** done (commit `52435cd`, 2026-10-08)
- **Reqs:** RF-06, RF-14
- **Depends on:** T-F09-01
- **Arquivos (alterar):** `frontend/src/components/task/TaskDetail.tsx`, `frontend/src/api/tasks.ts`, `frontend/src/test/TaskDetail.test.tsx`
- **O que fazer:** ao selecionar `DONE` no `StatusSelect` de uma tarefa com subtarefas pendentes, abrir `role="alertdialog"` (classes `.dialog*`) listando as pendentes com botões **Concluir**/**Cancelar**. Confirmar → `changeStatus(task.id, "DONE", true)`, `onChanged` atualiza o pai e as subtarefas locais viram `DONE`; Cancelar → fecha sem chamar a API (select volta ao status anterior). Sem pendentes → troca direta como hoje. `api/tasks.ts#changeStatus` ganha o parâmetro opcional `completeSubtasks` (omite quando ausente).
- **Pronto quando:** fluxo modal = listagem, concluir (API com flag + badges DONE), cancelar (nada muda), direto sem pendentes, e erro mantendo o modal aberto; testes de componente novos verdes.
- **Gate:** `npx oxlint` limpo; `npx vitest run` verde; `npx tsc -b && npx vite build` OK.

### T-F09-03 — Docs e rastreabilidade
- **Status:** done (2026-10-08)
- **Reqs:** DOC-01
- **Depends on:** T-F09-01, T-F09-02
- **Arquivos (alterar):** `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`
- **O que fazer:** RF-06 na matriz ganha a conclusão em cascata com confirmação (e o campo `completeSubtasks` no contrato), com os SHAs dos commits de código; STATE ganha a decisão do usuário (flag opcional + recusa cancela tudo) e o resumo do recurso; status das tasks aqui marcados `done` com commit.
- **Pronto quando:** matriz e STATE refletem o recurso com os commits; sem pendências não justificadas.
- **Gate:** `grep -c "done (parcial"` continua 0; `mvn test` e `npx vitest run` verdes.