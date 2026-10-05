| REQ | Feature | Task(s) | Código | Testes | Commit (assunto/hash) | Status |
|---|---|---|---|---|---|---|
| RNF-01 | F01 | T-F01-01, T-F01-04, T-F02-05b | `backend/pom.xml`, `AiTaskManagerApplication`, `common/error`, `common/config`, `common/web/HealthController` (503 quando o banco esta fora) | `mvn -q compile`; `GlobalExceptionHandlerTest`, `CorsConfigTest`, `HealthControllerTest` | 530102b, (T-F01-04), (T-F02-05b) | done |
| RNF-02 | F03,F04 | | | | | pending |
| RNF-03 | F01 | T-F01-02 | `frontend/src/router.tsx`, `AppLayout.tsx` | `npm run lint && npm run test && npm run build` | c89b825 | done |
| RNF-04 | F01,F05 | T-F01-03 | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` | `docker compose config && docker compose build` | b83deef | done |
| RNF-05 | F01,F05 | T-F01-02, T-F01-03 | `frontend/package.json`, `frontend/nginx.conf` | `npm run lint && npm run test && npm run build`; build das imagens | c89b825, b83deef | done |
| RF-01 | F02 | T-F02-02, T-F02-03, T-F02-04, T-F02-05c | `TaskService#create`, `TaskMapper#toDomain`, `task/domain/Task` (status inicial TODO), `TaskController#create` (201 + Location com o context-path, via `ServletUriComponentsBuilder`) | `TaskServiceTest.criarNasceComStatusInicialEPrioridadePadrao`, `criarComTituloEmBrancoEhRecusado`; `TaskControllerTest.criarRetorna201ComLocationIncluindoOContextPath`, `criarSemTituloRetorna400ComListaDeCampos` | (T-F02-02), (T-F02-03), (T-F02-04), (T-F02-05c) | done |
| RF-02 | F02 | T-F02-03, T-F02-04, T-F02-05f | `TaskService#findById`, `#list`, `#findAll`, `#findSubtasks`; `TaskFilter`; `TaskController#list` (ordenação `createdAt desc, id asc`), `#findById`, `#findSubtasks` | `TaskServiceTest.buscarPorIdDevolveATarefa`, `filtrarPorStatusRetornaSomenteODesejado`, `filtrosCompostosSeIntersecao`, `listarPaginadoRespeitaSizeEOrdenacao`; `TaskControllerTest.listarRetorna200ComEnvelopeDePagina`, `listarComFiltroDeStatusEPrioridadeRepassaAoService`, `listarUsaCreatedAtEIdComoDesempate`, `idEhUltimoCriterioDaOrdenacao`, `buscarPorIdRetorna200`, `listarSubtarefasRetorna200` | (T-F02-03), (T-F02-04), (T-F02-05f) | done |
| RF-03 | F02 | T-F02-03, T-F02-04 | `TaskService#update`, `TaskMapper#updateDomain`, `Task#updateContent`; `TaskController#update` | `TaskServiceTest.editarAtualizaConteudoEMantemStatus`, `editarComTituloEmBrancoEhRecusado`; `TaskControllerTest.editarRetorna200`, `editarSemTituloRetorna400`, `editarInexistenteRetorna404` | (T-F02-03), (T-F02-04) | done |
| RF-04 | F02 | T-F02-02, T-F02-03 | `UpdateTaskRequest` (sem campo status), `Task#updateContent` | `TaskMapperTest.updateDomainAlteraConteudoEMantemStatusEPai`; `TaskServiceTest.editarAtualizaConteudoEMantemStatus` | (T-F02-02), (T-F02-03) | done (parcial: PUT em T-F02-04) |
| RF-05 | F02 | T-F02-03, T-F02-04 | `TaskService#delete` (checa existencia; cascata de subtarefas no schema); `TaskController#delete` (204); confirmacao com a lista de subtarefas em T-F02-05 | `TaskServiceTest.excluirRemoveATarefa`, `excluirInexistenteRetorna404`, `excluirPaiRemoveSubtarefasPorCascata`; `TaskControllerTest.excluirRetorna204SemCorpo`, `excluirInexistenteRetorna404` | (T-F02-03), (T-F02-04) | done (parcial: confirmacao na UI em T-F02-05) |
| RF-06 | F02 | T-F02-03, T-F02-04 | `TaskService#changeStatus`, `Task#changeStatus` (DONE terminal); `TaskController#changeStatus` (PATCH) | `TaskServiceTest.alterarStatusPersisteNoBanco`, `voltarDeDoneParaInProgressoEhRecusado`, `reabrirDeDoneParaTodoEhPermitido`; `TaskControllerTest.alterarStatusRetorna200`, `transicaoRecusadaRetorna422`, `alterarStatusSemCampoRetorna400` | (T-F02-03), (T-F02-04) | done |
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
| RF-20 | F02 | T-F02-03, T-F02-04 | `TaskService#summary`, `TaskSummary`, `@Query` de contagem em `TaskRepository`; `TaskController#summary` | `TaskServiceTest.summaryContaCadaIndicador`, `summaryComBancoVazioTemTudoZero`, `summarySomaPorStatusIgualAoTotal`; `TaskControllerTest.summaryRetorna200ComOsCincoIndicadores` | (T-F02-03), (T-F02-04) | done (parcial: dashboard em T-F02-05) |
| RF-21 | F02 | T-F02-05 | `frontend/src/pages/DashboardPage.tsx`, `TasksPage.tsx`, `frontend/src/components/task/*` | `npm run lint && npm run test && npm run build` | | pending (T-F02-04 entregou a API que a tela consome) |
| RF-23 | F02 | T-F02-04 | `TaskController` (8 rotas sob `/tasks`, `context-path: /api`) | `TaskControllerTest` (27 testes de rota, status e shape) | (T-F02-04) | done |
| RF-24 | F02,F03,F04 | T-F02-04 | `TaskController` (contrato JSON de `TaskResponse`/`TaskSummary`/`PageResponse`) | `TaskControllerTest.listarRetorna200ComEnvelopeDePagina`, `summaryRetorna200ComOsCincoIndicadores` | (T-F02-04) | done (parcial: IA e assistente em F03/F04) |
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
| ERR-01 | F02,F05 | T-F01-04, T-F02-03, T-F02-04, T-F02-05a | `common/error/ResourceNotFoundException`, `GlobalExceptionHandler` (404 de recurso e 404 de rota via `NoResourceFoundException`); `TaskService#getOrThrow`; rotas de tarefas propagam o erro sem try/catch | `TaskServiceTest.buscarPorIdInexistenteRetorna404`, `excluirInexistenteRetorna404`, `editarTarefaInexistenteRetorna404`, `criarSubtaskDePaiInexistenteRetorna404`; `TaskControllerTest.buscarPorIdInexistenteRetorna404EmProblemDetail`, `listarSubtarefasDePaiInexistenteRetorna404`, `excluirInexistenteRetorna404`; `GlobalExceptionHandlerTest.rotaInexistenteDevolve404ENao500` | (T-F01-04), (T-F02-03), (T-F02-04), (T-F02-05a) | done (revisado em T-F05-02) |
| ERR-02 | F02,F05 | T-F01-04, T-F02-02, T-F02-04 | `common/error/GlobalExceptionHandler` (400 + lista `errors`), `FieldErrorItem`; constraints nos DTOs; validacao nativa de parametro no controller | `GlobalExceptionHandlerTest.beanValidationDevolve400ComListaDeCampos`; `TaskMapperTest.createRequestComTituloVazioApontaOCampo`, `updateStatusRequestExigeStatus`; `TaskControllerTest.criarSemTituloRetorna400ComListaDeCampos`, `criarComTituloLongoRetorna400ApontandoOCampo`, `listarComPageNegativaRetorna400ComCampo`, `criarComJsonMalformadoRetorna400SemDetalheInterno` | (T-F01-04), (T-F02-02), (T-F02-04) | done (revisado em T-F05-02) |
| ERR-03 | F03,F04,F05 | | | | | pending |
| ERR-04 | F03,F04,F05 | | | | | pending |
| ERR-05 | F03,F04,F05 | | | | | pending |
| ERR-06 | F02,F05 | T-F01-04, T-F02-04, T-F02-05a | `common/error/GlobalExceptionHandler` (500 com mensagem fixa; handler explicito para `DataAccessException`) | `GlobalExceptionHandlerTest.erroGenericoDevolve500ComMensagemFixaESemStackTrace`, `falhaDeBancoDevolve500ComMensagemFixaESemSql`; `TaskControllerTest.erroInesperadoRetorna500ComMensagemFixaESemStackTrace` (verifica que SQL, path e nome de classe nao vazam) | (T-F01-04), (T-F02-04), (T-F02-05a) | done (revisado em T-F05-02) |
| TST-01 | F02,F03,F04 | | | | | pending |
| TST-02 | F02,F03,F04 | | | | | pending |
| TST-03 | F03,F04 | | | | | pending |
| TST-02 | F02,F03,F04 | T-F02-04 | `TaskControllerTest` (`@WebMvcTest` com `TaskService` mockado), `GlobalExceptionHandlerTest` | `mvn -q test -Dtest=TaskControllerTest` — 27 testes verdes | (T-F02-04) | done (parcial: slices de IA e assistente em F03/F04) |
| TST-04 | F01,F02 | T-F01-04, T-F02-01, T-F02-02, T-F02-03 | `AiTaskManagerApplicationTests`, `TaskRepositoryTest`, `TaskServiceTest` (`@SpringBootTest` + Testcontainers Postgres), `TaskMapperTest`, `application-test.yml` | `mvn -q test` — 101 testes verdes | (T-F01-04), (T-F02-01), (T-F02-02), (T-F02-03), (T-F02-04) | done |
| TST-01 | F02,F03,F04 | T-F02-02 | `task/application/TaskMapper`, `task/api/dto/*` | `TaskMapperTest` (17 testes: round-trip, campos ausentes, coerência entidade<->DTO) | (T-F02-02) | done (parcial: mocks do modelo em F03/F04) |
| DOC-01 | F05 | | | | | pending |
| DEL-01 | F05 | | | | | pending |
| DEL-02 | F05 | | | | | pending |