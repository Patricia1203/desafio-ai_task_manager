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
- **Status:** done
- **Reqs:** RF-23, RF-24, RF-02, RF-07, ERR-01, ERR-02, ERR-06
- **Depende de:** T-F02-03
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/api/TaskController.java, backend/src/test/java/.../task/api/TaskControllerTest.java, backend/src/test/java/.../common/config/CorsConfigTest.java (ajuste de slice)
- **O que fazer:** GET /tasks (filtros status, priority, page, size), GET /tasks/summary, GET /tasks/{id}, POST /tasks (201 + Location), PUT /tasks/{id}, PATCH /tasks/{id}/status, DELETE /tasks/{id} (204), GET /tasks/{id}/subtasks. Erros no formato ProblemDetail.
- **Pronto quando:** todos os endpoints respondem com o status e corpo definidos em design.md; 400 lista os campos inválidos; 404 para id inexistente.
- **Testes:** TaskControllerTest (@WebMvcTest) para 200/201/204/400/404 e para o formato ProblemDetail, incluindo Location no 201.
- **Gate:** mvn -q test -Dtest=TaskControllerTest
- **Commit (rascunho):** `add: Controller REST de tarefas`
- **Nota:** **`/tasks/summary` é declarado antes de `/tasks/{id}`.** O `/{id}` com UUID casa qualquer string, então `summary` seria convertido para UUID e o summary ficaria inalcançável com 400. Ordem de mapeamento não é decoração aqui.
- **Nota 2:** **`@Validated` foi removido do controller.** Com ele, a validação de `@RequestParam` passa pelo proxy AOP e lança `ConstraintViolationException`, que não está mapeada: `page=-1` voltava **500** em vez de 400. Sem a anotação, a validação nativa do Spring 7 nos parâmetros do controller lança `HandlerMethodValidationException`, que o `GlobalExceptionHandler` já traduz. Não reintroduzir `@Validated` aqui sem mapear a exceção nova.
- **Nota 3:** `size` tem teto de 100 e `page` não pode ser negativo. Sem teto, `size=100000` vira um `SELECT` que derruba o banco por causa de um parâmetro de query.
- **Nota 4:** ordenação fixa em `createdAt desc` no controller. O filtro não expõe sort: a ordem é decisão de produto (mais recentes primeiro), não parâmetro do cliente.
- **Nota 5:** **`CorsConfigTest` foi fixado em `@WebMvcTest(HealthController.class)`.** Ele usava `@WebMvcTest` sem atributo, que carrega todos os controllers; com o `TaskController` no projeto, passou a exigir um `TaskService` e o contexto inteiro do teste falhou. Fixar o slice é a correção, não adicionar beans falsos.
- **Nota 6:** a exclusão em cascata pede confirmação na interface. O backend não exige flag: o `design.md` define "remove ou 404". A tela lista `GET /tasks/{id}/subtasks` e só então confirma. Ver T-F02-05.

### T-F02-05a — Handlers de status de infraestrutura do MVC
- **Status:** done
- **Reqs:** ERR-01, ERR-06
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/common/error/GlobalExceptionHandler.java`, `backend/src/test/java/com/desafio/taskmanager/common/error/ErrorProbeController.java`, `backend/src/test/java/com/desafio/taskmanager/common/error/GlobalExceptionHandlerTest.java`, `.specs/project/TRACEABILITY.md`
- **O que fazer:** mapear `NoResourceFoundException`/`NoHandlerFoundException` para 404, `HttpRequestMethodNotSupportedException` para 405 e `HttpMediaTypeNotSupportedException` para 415; adicionar handler explícito para `DataAccessException`.
- **Pronto quando:** os cinco testes novos passam e a suíte relevante continua verde.
- **Testes:** `rotaInexistenteDevolve404ENao500`, `metodoNaoPermitidoDevolve405ENao500`, `contentTypeInvalidoDevolve415ENao500`, `deleteNaRotaQueSoAceitaGetDevolve405`, `falhaDeBancoDevolve500ComMensagemFixaESemSql`.
- **Gate:** mvn -q test -Dtest=GlobalExceptionHandlerTest
- **Commit:** `fix: Mapear 404, 405 e 415 e tratar DataAccessException`
- **Nota:** **o `@ExceptionHandler(Exception.class)` é rede de segurança, não despachante.** Ele capturava as três exceções de infraestrutura do MVC e devolvia 500, então erro de navegação virava erro de servidor. Cada uma ganhou handler próprio; o Spring resolve pelo mais específico. O handler de `DataAccessException` devolve o mesmo status e a mesma mensagem fixa do catch-all, mas documenta a intenção e deixa o comportamento explícito caso mude.
- **Nota 2:** `falhaDeBancoDevolve500ComMensagemFixaESemSql` verifica que nem a URL do JDBC nem o nome do banco vazam. O `type` é `banco-indisponivel`, distinto de `erro-interno`, para o cliente distinguir causa transitória de bug.

### T-F02-05b — Health check com status 503
- **Status:** done
- **Reqs:** RNF-01
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/common/web/HealthController.java`, `backend/src/test/java/com/desafio/taskmanager/common/web/HealthControllerTest.java`, `.specs/project/TRACEABILITY.md`
- **O que fazer:** devolver HTTP 503 quando o `SELECT 1` falhar, mantendo `status: DOWN` no corpo.
- **Pronto quando:** teste cobre banco no ar (200/UP) e banco fora (503/DOWN).
- **Testes:** `HealthControllerTest.comBancoNoArDevolve200ComStatusUp`, `comBancoForaDevolve503ComStatusDown`, `comBancoForaNaoVazaDetalheDaFalha`.
- **Gate:** mvn -q test -Dtest=HealthControllerTest,CorsConfigTest
- **Commit:** `fix: Devolver 503 no health check quando o banco esta fora`
- **Nota:** **o corpo já dizia `DOWN` enquanto o status era 200, e o healthcheck do compose só olha o status HTTP.** Ou seja, o Compose considerava o serviço saudável com o Postgres fora do ar. O corpo não muda; o código de resposta passa a refletir a verdade.
- **Nota 2:** **`HealthControllerTest` é um arquivo novo, não uma extensão do `CorsConfigTest`.** O `CorsConfigTest` fixa o slice em `HealthController` só porque ele precisa do `JdbcTemplate` mockado; o mock é detalhe de CORS, não o que o teste exercita. Um teste que depende da infraestrutura alheia quebra quando a infra muda.

### T-F02-05c — Location do POST com o prefixo /api
- **Status:** done
- **Reqs:** RF-01
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/task/api/TaskController.java`, `backend/src/test/java/com/desafio/taskmanager/task/api/TaskControllerTest.java`, `.specs/project/TRACEABILITY.md`
- **O que fazer:** trocar `URI.create("/tasks/" + id)` por `ServletUriComponentsBuilder`, para o `Location` incluir o `context-path`.
- **Pronto quando:** o `Location` do 201 aponta para `/api/tasks/{id}`.
- **Testes:** `TaskControllerTest.criarRetorna201ComLocationIncluindoOContextPath`.
- **Gate:** mvn -q test -Dtest=TaskControllerTest
- **Commit:** `fix: Incluir o context-path no Location da criacao de tarefa`
- **Nota:** **o teste validava o bug.** `URI.create("/tasks/" + id)` ignora o `context-path: /api`, então o header apontava para um caminho que não existe. A asserção precisa mudar junto, senão o conserto quebra o teste e o teste quebra o conserto.
- **Nota 2:** **o teste declara `contextPath("/api")` e posta em `/api/tasks` de propósito.** O MockMvc não aplica o `context-path` do `application-test.yml` automaticamente; sem declarar, o `ServletUriComponentsBuilder` não teria prefixo nenhum para montar e o teste passaria vazio — validando de novo o caminho errado.

### T-F02-05d — Separação entre service e DTO de API
- **Status:** pending
- **Reqs:** RNF-20
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/task/application/TaskService.java`, `backend/src/main/java/com/desafio/taskmanager/task/api/TaskController.java`
- **O que fazer:** parar de devolver `task.api.dto.TaskResponse` a partir de `task.application`. O service devolve a entidade ou um objeto de aplicação neutro; o mapper fica no controller.
- **Pronto quando:** `task.application` não importa nada de `task.api`.
- **Testes:** ajustar `TaskServiceTest` para o novo tipo de retorno; `TaskControllerTest` continua cobrindo o JSON.
- **Gate:** mvn -q test -Dtest=TaskServiceTest
- **Commit (rascunho):** `refactor: Remover a dependencia de application sobre api`
- **Nota:** **decidir a direção antes de escrever código.** Duas saídas: mover os DTOs para `application` (churn alto em toda a feature, e passa a ser `application` quem define o formato de saída) ou deixar os DTOs em `api.dto` e fazer o controller mapear (o service devolve a entidade). A segunda é mais limpa em camadas e é a proposta.
- **Nota 2:** **`parent.getId()` foi testado e não causa N+1.** Sonda que roda `findAll()` + `TaskMapper#toResponse` fora de transação, com `open-in-view: false`: se o `getId()` tocasse o banco, a sessão estaria fechada e estouraria `LazyInitializationException`. Rodou limpo (6 tarefas mapeadas, 5 com `parentId`). O Hibernate devolve o identificador do proxy sem inicializar. Não criar coluna `parent_id` de leitura: seria mudança de schema sem problema para resolver.

### T-F02-05e — JSON em português
- **Status:** pending
- **Reqs:** RF-01, RF-20
- **Origem:** decisão do usuário
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/task/api/dto/*.java`, `backend/src/main/java/com/desafio/taskmanager/task/application/TaskSummary.java`, `backend/src/main/resources/db/migration/V2__create_tasks.sql` e testes
- **O que fazer:** renomear os campos do JSON para português.
- **Pronto quando:** o contrato inteiro responde em português e os testes refletem o nome novo.
- **Testes:** atualizar todas as asserções de `jsonPath`.
- **Gate:** mvn -q test
- **Commit (rascunho):** `refactor: Renomear o contrato JSON para portugues`
- **Nota (decisão do usuário):** **JSON em português, por ser projeto brasileiro.** Feito agora, antes do frontend consumir: depois seria breaking change com telas já escritas. Cuidado com os valores de enum, que também são string no JSON e com migration quando o valor mudar no banco.
- **Nota 2:** **[NEEDS CLARIFICATION] traceId.** O design não previa `traceId` e não há `traceId` em nenhum arquivo de `.specs`. Não é omissão do design, é decisão ainda não tomada: se entra, em qual header e em qual campo do ProblemDetail. `T-F05-02` fala em "preencher traceId e timestamp"; confirmar o formato antes de implementar.

### T-F02-05f — Ordenação estável na paginação
- **Status:** done
- **Reqs:** RF-02
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/src/main/java/com/desafio/taskmanager/task/api/TaskController.java`, `backend/src/test/java/com/desafio/taskmanager/task/api/TaskControllerTest.java`, `.specs/project/TRACEABILITY.md`
- **O que fazer:** acrescentar `id` como segundo criterio de ordenação, depois de `createdAt desc`.
- **Pronto quando:** o sort é totalmente determinado por `createdAt desc, id asc`.
- **Testes:** `TaskControllerTest.listarUsaCreatedAtEIdComoDesempate`, `idEhUltimoCriterioDaOrdenacao`.
- **Gate:** mvn -q test -Dtest=TaskControllerTest
- **Commit:** `fix: Desempatar a ordenacao por id na paginacao`
- **Nota:** **só `createdAt desc` não é ordenação.** Em empate de timestamp o Postgres não garante ordem estável entre consultas, e como a paginação usa OFFSET/LIMIT a mesma tarefa pode aparecer em duas páginas ou sumir de uma. O id é único e não muda depois de inserido.
- **Nota 2:** **o teste confere a posição, não só a presença.** Verificar que `id` existe no sort não basta: se viesse antes de `createdAt`, o desempate passaria a mandar na ordem principal. `idEhUltimoCriterioDaOrdenacao` usa `containsExactly` para travar a sequência.

### T-F02-05g — BOM, typos e documentação desatualizada
- **Status:** done
- **Reqs:** RNF-20
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-dev.yml`, `backend/src/main/resources/db/migration/V1__schema_base.sql`, Javadocs em `TaskService`, `TaskMapper`, `CorsProperties`, `Task`, `TaskRepository`
- **O que fazer:** remover BOM UTF-8 dos `.yml` e do `pom.xml`; corrigir `V1` que fala em `BIGSERIAL` para uma tabela com `UUID`; `createSubtask` que cita RF-03 em vez de RF-14; `@param parent` obsoleto em `updateDomain`; docstring de `CorsProperties.allowsOrigin`; typos `composedos` e `subtarea`.
- **Pronto quando:** nenhum arquivo versionado de `backend/` abre com BOM e nenhum javadoc aponta para requisito errado.
- **Testes:** suíte completa; a mudança é de byte inicial e de comentário, sem efeito de comportamento.
- **Gate:** mvn -q test
- **Commit:** `chore: Remover BOM e corrigir documentacao divergente`
- **Nota:** **o BOM some da tela mas quebra ferramentas.** `pom.xml` e os `.yml` começavam com `EF BB BF`; em YAML o parser pode falhar em espaço ou chave e o erro aponta para a linha errada. Removido com script, byte a byte, para não reescrever o conteúdo.
- **Nota 2:** **`@param parent` não é só texto errado: é um parâmetro que não existe.** `updateDomain` nunca recebeu `parent`; a documentação descrevia uma assinatura antiga.
- **Nota 3:** **o `RF-03` estava em dois lugares.** `TaskController#update` é RF-03 de verdade (edição de conteúdo); o errado era `createSubtask`, que é RF-14 (decomposição em subtarefas). Corrigir pelo grep sem ler o requisito teria trocado os dois.

### T-F02-05h — Fragilidade dos testes de integração
- **Status:** done (parte 1: asserção de tabelas e log)
- **Reqs:** TST-04
- **Origem:** revisão de código do backend
- **Arquivos (alterar):** `backend/src/test/java/com/desafio/taskmanager/AiTaskManagerApplicationTests.java`, `backend/src/main/resources/application.yml`
- **O que fazer:** trocar `containsExactlyInAnyOrder("flyway_schema_history", "tasks")` por `contains`, para não quebrar quando a F04 criar as tabelas de chat; mover o log DEBUG do `application.yml` base para o profile `dev`.
- **Pronto quando:** a suíte não depende da lista exata de tabelas e o profile default não loga em DEBUG.
- **Testes:** `AiTaskManagerApplicationTests.todoNegocioVeioDoFlywayEAindaEstaNoHistorico` (novo).
- **Gate:** mvn -q test
- **Commit:** `test: Desacoplar os testes da lista de tabelas e do log de dev`
- **Nota:** **a asserção de tabelas era uma bomba-relógio para a F04.** `containsExactlyInAnyOrder` falharia no dia em que o chat criasse a própria tabela, por um motivo sem relação com o que o teste verifica.
- **Nota 2:** **`contains` sozinho afrouxa o teste sem substituí-lo.** O que o teste realmente quer provar é que o Hibernate não criou nada (`ddl-auto=validate`). `todoNegocioVeioDoFlywayEAindaEstaNoHistorico` mantém essa afirmação: toda tabela presente tem que estar no conjunto que as migrations criaram. Frouxidão sem contrapartida vira perda de cobertura silenciosa.
- **Nota 3:** **o item de container compartilhado e teste de domínio puro foi para T-F02-05i**, para não misturar duas causas diferentes no mesmo commit.
- **Nota 4:** `application.yml` agora loga `INFO` na aplicação; `application-dev.yml` mantém `DEBUG` e `org.hibernate.SQL: DEBUG`.

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
- **Nota (decisão do usuário):** **a exclusão em cascata pede confirmação na interface, listando as subtarefas que serão arrastadas.** Ao clicar em excluir, a tela busca `GET /tasks/{id}/subtasks`; havendo filhas, mostra os títulos numa caixa de confirmação antes de chamar o `DELETE`. Sem subtarefas, confirma direto. O backend não ganha parâmetro de confirmação — o contrato é "remove ou 404" e a lista já está disponível. Teste obrigatório: `TaskDetail` mostra os títulos das subtarefas no diálogo e o DELETE só é disparado após confirmar.