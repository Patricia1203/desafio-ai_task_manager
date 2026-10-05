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
- **Status:** done
- **Reqs:** RF-07, ERR-02
- **Depende de:** T-F02-01
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/api/dto/TaskResponse.java, CreateTaskRequest.java, UpdateTaskRequest.java, UpdateStatusRequest.java, PageResponse.java, backend/src/main/java/com/desafio/taskmanager/task/application/TaskMapper.java, backend/src/test/java/.../task/application/TaskMapperTest.java
- **O que fazer:** DTOs como `record`, com Bean Validation (título obrigatório e limitado, descrição com tamanho máximo, prazo, enums). Mapper explícito entidade <-> DTO; entidade JPA nunca exposta na API.
- **Pronto quando:** DTOs validam entradas inválidas e o mapper cobre todos os campos de TaskResponse.
- **Testes:** TaskMapperTest (round-trip completo, campos ausentes) e testes de validação do CreateTaskRequest.
- **Gate:** mvn -q test -Dtest=TaskMapperTest
- **Commit (rascunho):** `add: DTOs e mapper de Task`
- **Nota:** limite de título 200 (o mesmo do `VARCHAR(200)`) e descrição 5000, com as mensagens em português porque o `GlobalExceptionHandler` joga a mensagem da constraint direto no ProblemDetail.
- **Nota 2:** `TaskResponse` expõe `parentId` (UUID) e nunca o `Task parent`, para o JSON da lista não arrastar a entidade carregada via LAZY.
- **Nota 3:** **o status não é campo de `UpdateTaskRequest`.** PUT é substituição de conteúdo; a transição de status passa por `PATCH /tasks/{id}/status` e pela regra de domínio. Se o PUT aceitasse status, o mapper burlaria `changeStatus` e a invariante DONE-terminal ficaria sem dono.
- **Nota 4:** `PageResponse.of(Page<T>)` é o único ponto que conhece o `Page` do Spring Data. O envelope (`content`, `page`, `size`, `totalItems`, `totalPages`, `first`, `last`) é genérico e já serve para as mensagens do assistente em F03.
- **Nota 5:** `toSubtask` existe separado de `toDomain` porque a decomposição por IA (RF-14, F03) cria subtarefa com pai obrigatório — deixar o pai explícito na assinatura impede que um caminho da IA crie tarefa de topo por engano.
- **Nota 6:** `Validator.validate` devolve `Set`, que não tem posição fixa. O teste usa um helper `unica(Set)` que valida o tamanho e devolve `iterator().next()`; escrever `violations.get(0)` no lugar dá erro de compilação e induz a mudar o tipo errado.

### T-F02-03 — Service CRUD, filtros, status e summary
- **Status:** done
- **Reqs:** RF-01, RF-02, RF-03, RF-04, RF-05, RF-06, RF-08, RF-09, RF-20, ERR-01
- **Depende de:** T-F02-02
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/application/TaskService.java, backend/src/main/java/com/desafio/taskmanager/task/application/dto/TaskSummary.java, backend/src/main/java/com/desafio/taskmanager/task/application/dto/TaskFilter.java, backend/src/main/java/com/desafio/taskmanager/common/error/ResourceNotFoundException.java, backend/src/test/java/.../task/application/TaskServiceTest.java
- **O que fazer:** Regras de negócio: criação com status inicial TODO e createdAt automático; alteração de status; listagem com filtros de status/prioridade e paginação; summary com total, pendentes, em andamento, concluídas e alta prioridade; tarefa inexistente vira ResourceNotFoundException.
- **Pronto quando:** todas as regras de F02 spec produzem o resultado esperado; summary bate com os dados persistidos.
- **Testes:** TaskServiceTest cobrindo criar com status TODO, editar, excluir, alterar status, listagem filtrada, subtarefas, summary e exceção 404 para id inexistente.
- **Gate:** mvn -q test -Dtest=TaskServiceTest
- **Commit (rascunho):** `add: Service de tarefas com CRUD, filtros e summary`
- **Nota:** **exclusão de tarefa com subtarefas é cascata, e isso é decisão, não esquecimento.** O `ON DELETE CASCADE` de `V2__create_tasks.sql` já estava no banco; o service só não checa nada antes de apagar. A alternativa (recusar com 422 e obrigar a mover/excluir as filhas antes) muda o `delete` para uma transação que conta as filhas — trocar depois significa remover a linha do cascade na próxima migration, não reescrever a feature. Registrado em `STATE.md`.
- **Nota 2:** `TaskFilter` é um record com `status` e `priority` opcionais, sem pageable — a paginação é argumento separado de `TaskService#list(filter, pageable)`. Assim o mesmo filtro serve para a listagem paginada da tela e para um `findAll` do assistente (F04), que não pagina.
- **Nota 3:** o summary é uma classe de resultado (`TaskSummary`) calculada a partir das `@Query` de contagem já prontas em `TaskRepository`, e não um `Map<String, Long>`. Record com nome deixa o contrato do dashboard explícito e o teste falha em silêncio se o nome do campo mudar.
- **Nota 4:** a exclusão checa existência antes de apagar. `repository.delete(id)` sem `findById` responderia 204 para um id inexistente, e o requisito ERR-01 pede 404.
- **Nota 5:** o teste `excluirPaiRemoveSubtarefasPorCascata` fixa a comportamento escolhido no banco. É o teste que vai falhar se alguém trocar a cascata por bloqueio sem perceber que mudou a regra.
- **Nota 6:** `TaskService` é `@Transactional(readOnly = true)` na classe e `@Transactional` nos métodos de escrita. Sem isso o `getOrThrow` + alteração + `save`_from_leitura_precisariam de transação própria por chamada.
- **Nota 7:** o teste `alterarParaOMesmoStatusNaoFalhaNemMexeEmUpdatedAt` relê a tarefa antes de agir, porque `timestamptz` guarda microssegundos e `Instant` guarda nanos: comparar o valor em memória com o relido falha por precisão, não por lógica.

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