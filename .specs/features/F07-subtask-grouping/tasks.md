# tasks.md — F07-subtask-grouping

> Feature criada em 2026-10-07 a pedido do usuário (request: "as subtarefas parecem tarefas
> independentes e não têm vínculo com a original"). O usuário já autorizou a implementação após a
> criação das tasks. Decisão de apresentação e assimetrias registradas em `design.md`.

### T-F07-01 — Lista pública só com tarefas-raiz
- **Status:** pending
- **Reqs:** RF-02, RF-13, RF-14
- **Depends on:** —
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/task/application/TaskService.java`, testes (repository/service/controller)
- **O que fazer:** `toSpecification` passa a sempre excluir subtarefas (`parent is null`), compondo com status/prioridade. `GET /tasks` e `TaskService.findAll` passam a devolver só tarefas-raiz; subtarefas continuam acessíveis por `GET /tasks/{id}` e `GET /tasks/{id}/subtasks`. Ajustar os testes de listagem que esperavam filhas na lista.
- **Pronto quando:** raiz com subtarefa não aparece na lista; a filha continua acessível pelo id e pelo endpoint de subtarefas; filtros de status/prioridade seguem funcionando sobre as raízes.
- **Gate:** `mvn test` verde; teste de integração prova que `GET /tasks` não devolve `parentId` não nulo.

### T-F07-02 — `subtaskCount` na resposta da lista
- **Status:** pending
- **Reqs:** RF-14
- **Depends on:** T-F07-01
- **Arquivos (alterar):** `TaskResponse.java`, `TaskMapper.java`, `TaskService.java` (+ repository), testes
- **O que fazer:** `TaskResponse` ganha `long subtaskCount`. O `TaskService.list` calcula a contagem agrupada por pai numa única query e injeta no mapeamento (sem N+1); `TaskMapper.toResponse` aceita o mapa opcional de contagens (outros usos passam nulo → 0). Contrato em inglês.
- **Pronto quando:** a lista traz `subtaskCount` correto por raiz, sem consulta por linha (verificável por contagem de queries no teste de integração ou por construção).
- **Gate:** `mvn test` verde; teste prova o count agrupado.

### T-F07-03 — Lista frontend com selo de subtarefas
- **Status:** pending
- **Reqs:** RF-14
- **Depends on:** T-F07-02
- **Arquivos (alterar):** `frontend/src/types/task.ts`, `frontend/src/components/task/TaskList.tsx`, CSS, testes de componente
- **O que fazer:** o tipo ganha `subtaskCount`; a linha da lista mostra o selo "N subtarefas" quando `subtaskCount > 0`, ao lado dos badges de status/prioridade (rótulo em português, conforme F06).
- **Pronto quando:** linha com subtarefas exibe o selo; linha sem, não.
- **Gate:** `npx oxlint` limpo; `npx vitest run` verde.

### T-F07-04 — Detalhe agrupado + vínculo do pai
- **Status:** pending
- **Reqs:** RF-13, RF-14
- **Depends on:** T-F07-02
- **Arquivos (alterar):** `frontend/src/components/task/TaskDetail.tsx`, `frontend/src/pages/TasksPage.tsx`, CSS, testes
- **O que fazer:** (a) `TaskDetail` carrega `getSubtasks` ao abrir e renderiza o bloco "Subtarefas (N)" com título, status, prioridade e prazo por item, clicável para abrir a filha; o aviso de exclusão reusa essa lista já carregada. (b) Quando a tarefa aberta é subtarefa (`parentId`), busca o pai (`getTask(task.parentId)`) e mostra o selo "Subtarefa de <título>", clicável para voltar ao pai. Navegação pai↔filha sem sair do modo detalhe.
- **Pronto quando:** detalhe de uma raiz lista as subtarefas agrupadas; detalhe de uma filha mostra o vínculo com o pai e permite voltar a ele.
- **Gate:** `npx oxlint` limpo; `npx vitest run` verde; `npx tsc -b && npx vite build` OK.

### T-F07-05 — Docs e rastreabilidade
- **Status:** pending
- **Reqs:** DOC-01
- **Depends on:** T-F07-01, T-F07-04
- **Arquivos (alterar):** `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`, `README.md` (se necessário), commits de docs por task
- **O que fazer:** RF-13/RF-14 na matriz ganham o agrupamento visível (bloco de subtarefas no detalhe e lista só de raízes) com os SHAs dos commits de código; STATE atualizado após cada task; status das tasks aqui marcado com datá-done e commit.
- **Pronto quando:** matriz reflete o vínculo com os commits; STATE sem pendência não justificada.
- **Gate:** `grep -c "done (parcial"` continua 0.