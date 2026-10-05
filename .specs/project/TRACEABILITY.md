| REQ | Feature | Task(s) | Código | Testes | Commit (assunto/hash) | Status |
|---|---|---|---|---|---|---|
| RNF-01 | F01 | T-F01-01, T-F01-04 | `backend/pom.xml`, `AiTaskManagerApplication`, `common/error`, `common/config` | `mvn -q compile`; `GlobalExceptionHandlerTest`, `CorsConfigTest` | 530102b, (T-F01-04) | done |
| RNF-02 | F03,F04 | | | | | pending |
| RNF-03 | F01 | T-F01-02 | `frontend/src/router.tsx`, `AppLayout.tsx` | `npm run lint && npm run test && npm run build` | c89b825 | done |
| RNF-04 | F01,F05 | T-F01-03 | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` | `docker compose config && docker compose build` | b83deef | done |
| RNF-05 | F01,F05 | T-F01-02, T-F01-03 | `frontend/package.json`, `frontend/nginx.conf` | `npm run lint && npm run test && npm run build`; build das imagens | c89b825, b83deef | done |
| RF-01 | F02 | | | | | pending |
| RF-02 | F02 | | | | | pending |
| RF-03 | F02 | | | | | pending |
| RF-04 | F02 | | | | | pending |
| RF-05 | F02 | | | | | pending |
| RF-06 | F02 | | | | | pending |
| RF-07 | F02 | | | | | pending |
| RF-08 | F02 | | | | | pending |
| RF-09 | F02 | | | | | pending |
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
| RF-20 | F02 | | | | | pending |
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
| RNF-21 | F01 | T-F01-04 | `common/config/WebConfig`, `common/config/CorsProperties` | `CorsConfigTest` (origem permitida e não permitida, preflight) | (T-F01-04) | done |
| ERR-01 | F02,F05 | | | | | pending |
| ERR-02 | F02,F05 | T-F01-04 | `common/error/GlobalExceptionHandler` (400 + lista `errors`), `FieldErrorItem` | `GlobalExceptionHandlerTest.beanValidationDevolve400ComListaDeCampos` | (T-F01-04) | done (parcial, revisado em T-F05-02) |
| ERR-03 | F03,F04,F05 | | | | | pending |
| ERR-04 | F03,F04,F05 | | | | | pending |
| ERR-05 | F03,F04,F05 | | | | | pending |
| ERR-06 | F02,F05 | T-F01-04 | `common/error/GlobalExceptionHandler` (500 com mensagem fixa) | `GlobalExceptionHandlerTest.erroGenericoDevolve500ComMensagemFixaESemStackTrace` | (T-F01-04) | done (parcial, revisado em T-F05-02) |
| TST-01 | F02,F03,F04 | | | | | pending |
| TST-02 | F02,F03,F04 | | | | | pending |
| TST-03 | F03,F04 | | | | | pending |
| TST-04 | F01,F02 | T-F01-04 | `AiTaskManagerApplicationTests` (`@SpringBootTest` + Testcontainers Postgres), `application-test.yml` | `mvn -q test` — 14 testes verdes | (T-F01-04) | done |
| DOC-01 | F05 | | | | | pending |
| DEL-01 | F05 | | | | | pending |
| DEL-02 | F05 | | | | | pending |