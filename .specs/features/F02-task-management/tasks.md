# tasks.md — F02-task-management

### T-F02-01 — Entidade, migração e repositório Task
- **Status:** done
- **Reqs:** RF-07, RF-08, RF-09, RNF-21, TST-04
- **Depende de:** T-F01-04
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/domain/Task.java, backend/src/main/java/com/desafio/taskmanager/task/domain/TaskStatus.java, backend/src/main/java/com/desafio/taskmanager/task/domain/TaskPriority.java, backend/src/main/java/com/desafio/taskmanager/task/infra/TaskRepository.java, backend/src/main/resources/db/migration/V2__create_tasks.sql, backend/src/test/java/.../task/infra/TaskRepositoryTest.java
- **O que fazer:** Modelar Task com id UUID, título, descrição, status, prioridade, prazo, createdAt, updatedAt e parentTaskId (auto-relacionamento para subtarefas). Índices em status, priority, due_date e parent_task_id. Mapeamento JPA explícito; transições de status e demais invariantes ficam nos métodos de domínio.
- **Pronto quando:** migração aplicada em Postgres real; entidade lida e gravada pelo repositório com índices válidos sob `ddl-auto=validate`.
- **Testes:** TaskRepositoryTest (save/find, filtro por status/prioridade, busca de subtarefas por parentTaskId) com Testcontainers.
- **Gate:** mvn -q test -Dtest=TaskRepositoryTest
- **Commit (rascunho):** `add: Entidade e migração de Task`
- **Nota:** `parentTaskId` do design virou `Task.parent` com `@ManyToOne(fetch = LAZY)`, e a coluna é `parent_id`. O self-reference tem `ON DELETE CASCADE`: excluir a tarefa pai remove as subtarefas.
- **Nota 2:** **DONE é terminal.** `Task#changeStatus` só permite sair de DONE para TODO (reabrir). Voltar direto para IN_PROGRESS lança `BusinessRuleException` (422). Regra documentada em `STATE.md` como [ASSUMPTION] — se o desafio permitir reabrir direto, é uma linha no método.
- **Nota 3:** id é `UUID` gerado em Java (`UUID.randomUUID()` na factory da entidade), não no banco, para o id existir antes do INSERT e a subtarefa poder referenciar o pai já na mesma transação.
- **Nota 4:** `@BeforeEach repository.deleteAll()` nos testes: os testes de integração compartilham o mesmo container e as contagens do summary não podem depender da ordem de execução.
- **Nota 5:** o teste `atualizaStatusNoBanco` usa `saveAndFlush` a cada mudança de status, não `flush`. Sem `@Transactional` no teste a entidade fica detached e `flush()` sozinho não gera UPDATE.

### T-F02-02 — DTOs, mapper e validações
- **Status:** pending
- **Reqs:** RF-07, ERR-02
- **Depende de:** T-F02-01
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/api/dto/TaskResponse.java, CreateTaskRequest.java, UpdateTaskRequest.java, UpdateStatusRequest.java, PageResponse.java, backend/src/main/java/com/desafio/taskmanager/task/application/TaskMapper.java, backend/src/test/java/.../task/application/TaskMapperTest.java
- **O que fazer:** DTOs como `record`, com Bean Validation (título obrigatório e limitado, descrição com tamanho máximo, prazo, enums). Mapper explícito entidade <-> DTO; entidade JPA nunca exposta na API.
- **Pronto quando:** DTOs validam entradas inválidas e o mapper cobre todos os campos de TaskResponse.
- **Testes:** TaskMapperTest (round-trip completo, campos ausentes) e testes de validação do CreateTaskRequest.
- **Gate:** mvn -q test -Dtest=TaskMapperTest
- **Commit (rascunho):** `add: DTOs e mapper de Task`

### T-F02-03 — Service CRUD, filtros, status e summary
- **Status:** pending
- **Reqs:** RF-01, RF-02, RF-03, RF-04, RF-05, RF-06, RF-08, RF-09, RF-20, ERR-01
- **Depende de:** T-F02-02
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/application/TaskService.java, backend/src/main/java/com/desafio/taskmanager/task/application/dto/TaskSummary.java, backend/src/main/java/com/desafio/taskmanager/task/application/dto/TaskFilter.java, backend/src/main/java/com/desafio/taskmanager/common/error/ResourceNotFoundException.java, backend/src/test/java/.../task/application/TaskServiceTest.java
- **O que fazer:** Regras de negócio: criação com status inicial TODO e createdAt automático; alteração de status; listagem com filtros de status/prioridade e paginação; summary com total, pendentes, em andamento, concluídas e alta prioridade; tarefa inexistente vira ResourceNotFoundException.
- **Pronto quando:** todas as regras de F02 spec produzem o resultado esperado; summary bate com os dados persistidos.
- **Testes:** TaskServiceTest cobrindo criar com status TODO, editar, excluir, alterar status, listagem filtrada, subtarefas, summary e exceção 404 para id inexistente.
- **Gate:** mvn -q test -Dtest=TaskServiceTest
- **Commit (rascunho):** `add: Service de tarefas com CRUD, filtros e summary`

### T-F02-04 — Controller REST de tarefas
- **Status:** pending
- **Reqs:** RF-23, RF-24, RF-02, RF-07, ERR-01, ERR-02, ERR-06
- **Depende de:** T-F02-03
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/api/TaskController.java, backend/src/test/java/.../task/api/TaskControllerTest.java
- **O que fazer:** GET /tasks (filtros status, priority, page, size), GET /tasks/summary, GET /tasks/{id}, POST /tasks (201 + Location), PUT /tasks/{id}, PATCH /tasks/{id}/status, DELETE /tasks/{id} (204), GET /tasks/{id}/subtasks. Erros no formato ProblemDetail.
- **Pronto quando:** todos os endpoints respondem com o status e corpo definidos em design.md; 400 lista os campos inválidos; 404 para id inexistente.
- **Testes:** TaskControllerTest (@WebMvcTest) para 200/201/204/400/404 e para o formato ProblemDetail, incluindo Location no 201.
- **Gate:** mvn -q test -Dtest=TaskControllerTest
- **Commit (rascunho):** `add: Controller REST de tarefas`

### T-F02-05 — Frontend Dashboard e Tarefas
- **Status:** pending
- **Reqs:** RF-20, RF-21, RNF-03
- **Depende de:** T-F02-04
- **Arquivos (criar/alterar):** frontend/src/api/tasks.ts, frontend/src/types/task.ts, frontend/src/pages/DashboardPage.tsx, frontend/src/pages/TasksPage.tsx, frontend/src/components/task/TaskForm.tsx, frontend/src/components/task/TaskList.tsx, frontend/src/components/task/TaskDetail.tsx, frontend/src/components/task/StatusSelect.tsx, frontend/src/components/common/AsyncState.tsx, frontend/src/test/*.test.tsx
- **O que fazer:** Dashboard com os indicadores do summary; tela de tarefas com lista filtrável, formulário criar/editar, detalhe com alteração de status e exclusão; estados de loading, erro e vazio em todas as telas; botões de ação desabilitados durante chamadas.
- **Pronto quando:** `npm run lint`, `npm run test` e `npm run build` passam; os fluxos críticos estão cobertos por teste.
- **Testes:** TaskForm (validação e submit), TaskList (render, filtro, vazio), TaskDetail (mudança de status e exclusão) com a camada api/ mockada.
- **Gate:** cd frontend && npm run lint && npm run test && npm run build
- **Commit (rascunho):** `add: Telas de Dashboard e Tarefas com CRUD completo`