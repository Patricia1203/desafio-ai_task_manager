| REQ | Feature | Task(s) | Código | Testes | Commit (assunto/hash) | Status |
|---|---|---|---|---|---|---|
| RNF-01 | F01 | T-F01-01, T-F01-04 | `backend/pom.xml`, `AiTaskManagerApplication`, `common/error`, `common/config` | `mvn -q compile`; `GlobalExceptionHandlerTest`, `CorsConfigTest` | 530102b, (T-F01-04) | done |
| RNF-02 | F03,F04 | | | | | pending |
| RNF-03 | F01 | T-F01-02 | `frontend/src/router.tsx`, `AppLayout.tsx` | `npm run lint && npm run test && npm run build` | c89b825 | done |
| RNF-04 | F01,F05 | T-F01-03 | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` | `docker compose config && docker compose build` | b83deef | done |
| RNF-05 | F01,F05 | T-F01-02, T-F01-03 | `frontend/package.json`, `frontend/nginx.conf` | `npm run lint && npm run test && npm run build`; build das imagens | c89b825, b83deef | done |
| RF-01 | F02 | T-F02-02, T-F02-03 | `TaskService#create`, `TaskMapper#toDomain`, `task/domain/Task` (status inicial TODO) | `TaskServiceTest.criarNasceComStatusInicialEPrioridadePadrao`, `criarComTituloEmBrancoEhRecusado` | (T-F02-02), (T-F02-03) | done (parcial: POST em T-F02-04) |
| RF-02 | F02 | T-F02-03 | `TaskService#findById`, `#list`, `#findAll`, `#findSubtasks`; `TaskFilter` | `TaskServiceTest.buscarPorIdDevolveATarefa`, `filtrarPorStatusRetornaSomenteODesejado`, `filtrosCompostosSeIntersecao`, `listarPaginadoRespeitaSizeEOrdenacao` | (T-F02-03) | done (parcial: endpoints em T-F02-04) |
| RF-03 | F02 | T-F02-03 | `TaskService#update`, `TaskMapper#updateDomain`, `Task#updateContent` | `TaskServiceTest.editarAtualizaConteudoEMantemStatus`, `editarComTituloEmBrancoEhRecusado` | (T-F02-03) | done (parcial: PUT em T-F02-04) |
| RF-04 | F02 | T-F02-02, T-F02-03 | `UpdateTaskRequest` (sem campo status), `Task#updateContent` | `TaskMapperTest.updateDomainAlteraConteudoEMantemStatusEPai`; `TaskServiceTest.editarAtualizaConteudoEMantemStatus` | (T-F02-02), (T-F02-03) | done (parcial: PUT em T-F02-04) |
| RF-05 | F02 | T-F02-03 | `TaskService#delete` (checa existencia; cascata de subtarefas no schema) | `TaskServiceTest.excluirRemoveATarefa`, `excluirInexistenteRetorna404`, `excluirPaiRemoveSubtarefasPorCascata` | (T-F02-03) | done (parcial: DELETE em T-F02-04) |
| RF-06 | F02 | T-F02-03 | `TaskService#changeStatus`, `Task#changeStatus` (DONE terminal) | `TaskServiceTest.alterarStatusPersisteNoBanco`, `voltarDeDoneParaInProgressoEhRecusado`, `reabrirDeDoneParaTodoEhPermitido` | (T-F02-03) | done (parcial: PATCH em T-F02-04) |
| RF-07 | F02 | T-F02-01, T-F02-02 | `task/domain/Task`, `TaskStatus`, `TaskPriority`, `V2__create_tasks.sql`; `task/api/dto/{Create,Update}TaskRequest`, `TaskResponse`, `PageResponse`, `task/application/TaskMapper` | `TaskRepositoryTest.gravaELeComTodosOsCampos`, `tarefaNovaNasceComStatusInicialEPrioridadePadrao`; `TaskMapperTest.toDomainCriaTarefaComTodosOsCampos`, `toResponseCobreTodosOsCamposDaEntidade`, `roundTripPreservaTodosOsCampos` | (T-F02-01), (T-F02-02) | done (parcial: endpoints em T-F02-04) |
| RF-08 | F02 | T-F02-01 | `task/domain/TaskStatus`, `Task#changeStatus` | `TaskRepositoryTest.atualizaStatusNoBanco` | (T-F02-01) | done (parcial: PATCH em T-F02-04) |
| RF-09 | F02 | T-F02-01 | `task/domain/TaskPriority`, `task/infra/TaskRepository` (índices e filtros) | `TaskRepositoryTest.filtraPorPrioridade`, `filtraComSpecificationCompostaEOrdena`, `paginaResultados` | (T-F02-01) | done (parcial: endpoint em T-F02-04) |
| RF-10 | F03 | | | | | pending |
| RF-11 | F03 | | | | | pending |
| RF-12 | F03 | | | | | pending |
| RF-13 | F03 | | | | | pending |
| RF-14 | F03 | | | | | pending |
| RF-15 | F04 | | | | | pending |
| RF-16 | F04 | | | | | pending |
| RF-17 | F04 | | | | | pending |
| RF-18 | F04 | | | | | pending |
| RF-19 | F04 | | | | | pending |
| RF-20 | F02 | T-F02-03 | `TaskService#summary`, `TaskSummary`, `@Query` de contagem em `TaskRepository` | `TaskServiceTest.summaryContaCadaIndicador`, `summaryComBancoVazioTemTudoZero`, `summarySomaPorStatusIgualAoTotal` | (T-F02-03) | done (parcial: endpoint em T-F02-04, dashboard em T-F02-05) |
| RF-21 | F02 | | | | | pending |
| RF-22 | F04 | | | | | pending |
| RF-23 | F02 | | | | | pending |
| RF-24 | F03,F04 | | | | | pending |
| RNF-10 | F03,F04 | | | | | pending |
| RNF-11 | F03,F04 | | | | | pending |
| RNF-12 | F03,F04 | | | | | pending |
| RNF-13 | F03,F04 | | | | | pending |
| RNF-14 | F03,F04 | | | | | pending |
| RNF-15 | F03,F04 | | | | | pending |
| RNF-20 | F01 | T-F01-01, T-F01-04 | `application.yml`, `common/config/WebConfig`, `common/config/CorsProperties`, `common/error/GlobalExceptionHandler` | `AiTaskManagerApplicationTests` (contexto sobe, schema Flyway aplicado) | 530102b, (T-F01-04) | done |
| RNF-21 | F01,F02 | T-F01-04, T-F02-01 | `common/config/WebConfig`, `common/config/CorsProperties`; `V2__create_tasks.sql` (índices em status/priority/due_date/parent_id) | `CorsConfigTest` (origem permitida e não permitida, preflight); `TaskRepositoryTest.filtraPorStatus`, `filtraPorPrioridade` | (T-F01-04), (T-F02-01) | done (parcial: telas em T-F02-05) |
| ERR-01 | F02,F05 | T-F01-04, T-F02-03 | `common/error/ResourceNotFoundException`, `GlobalExceptionHandler` (404); `TaskService#getOrThrow` | `TaskServiceTest.buscarPorIdInexistenteRetorna404`, `excluirInexistenteRetorna404`, `editarTarefaInexistenteRetorna404`, `criarSubtaskDePaiInexistenteRetorna404` | (T-F01-04), (T-F02-03) | done (parcial, controller em T-F02-04, revisado em T-F05-02) |
| ERR-02 | F02,F05 | T-F01-04, T-F02-02 | `common/error/GlobalExceptionHandler` (400 + lista `errors`), `FieldErrorItem`; constraints em `CreateTaskRequest`/`UpdateTaskRequest`/`UpdateStatusRequest` (mensagens em portugues) | `GlobalExceptionHandlerTest.beanValidationDevolve400ComListaDeCampos`; `TaskMapperTest.createRequestComTituloVazioApontaOCampo`, `createRequestComTituloLongoApontaOCampoComMensagem`, `updateStatusRequestExigeStatus` | (T-F01-04), (T-F02-02) | done (parcial, controller em T-F02-04, revisado em T-F05-02) |
| ERR-03 | F03,F04,F05 | | | | | pending |
| ERR-04 | F03,F04,F05 | | | | | pending |
| ERR-05 | F03,F04,F05 | | | | | pending |
| ERR-06 | F02,F05 | T-F01-04 | `common/error/GlobalExceptionHandler` (500 com mensagem fixa) | `GlobalExceptionHandlerTest.erroGenericoDevolve500ComMensagemFixaESemStackTrace` | (T-F01-04) | done (parcial, revisado em T-F05-02) |
| TST-01 | F02,F03,F04 | | | | | pending |
| TST-02 | F02,F03,F04 | | | | | pending |
| TST-03 | F03,F04 | | | | | pending |
| TST-04 | F01,F02 | T-F01-04, T-F02-01, T-F02-02, T-F02-03 | `AiTaskManagerApplicationTests`, `TaskRepositoryTest`, `TaskServiceTest` (`@SpringBootTest` + Testcontainers Postgres), `TaskMapperTest`, `application-test.yml` | `mvn -q test` — 74 testes verdes | (T-F01-04), (T-F02-01), (T-F02-02), (T-F02-03) | done |
| TST-01 | F02,F03,F04 | T-F02-02 | `task/application/TaskMapper`, `task/api/dto/*` | `TaskMapperTest` (17 testes: round-trip, campos ausentes, coerência entidade<->DTO) | (T-F02-02) | done (parcial: mocks do modelo em F03/F04) |
| DOC-01 | F05 | | | | | pending |
| DEL-01 | F05 | | | | | pending |
| DEL-02 | F05 | | | | | pending |