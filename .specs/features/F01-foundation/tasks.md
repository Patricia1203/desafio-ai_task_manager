# tasks.md — F01-foundation

### T-F01-01 — Bootstrap backend (Spring Boot 4, Java 21)
- **Status:** done
- **Reqs:** RNF-01, RNF-20
- **Depende de:** (nenhuma)
- **Arquivos (criar/alterar):** backend/pom.xml, backend/src/main/java/com/desafio/taskmanager/AiTaskManagerApplication.java, backend/src/main/resources/application.yml, backend/src/main/resources/application-dev.yml
- **O que fazer:** Inicializar projeto Maven com dependências Spring Boot 4, Web, Data JPA, Validation, PostgreSQL, Flyway e Spring AI (versões verificadas antes de uso).
- **Pronto quando:** `mvn compile` passa sem erros; estrutura de pacotes por feature preparada.
- **Testes:** sem teste unitário nesta task (o smoke test com Testcontainers entra em T-F01-04).
- **Gate:** mvn -q compile
- **Commit (rascunho):** `add: Bootstrap backend Spring Boot 4.1.1 + Spring AI 2.0.1 (Java 21)`
- **Nota:** versões reais Boot 4.1.1 / Spring AI 2.0.1 / Testcontainers 2.0.5 (verificadas no Maven Central). Commit: 530102b.

### T-F01-02 — Skeleton frontend (Vite + React + TS)
- **Status:** done
- **Reqs:** RNF-03, RNF-05
- **Depende de:** T-F01-01
- **Arquivos (criar/alterar):** frontend/package.json, frontend/vite.config.ts, frontend/index.html, frontend/src/main.tsx, frontend/src/router.tsx, frontend/src/index.css, frontend/src/api/client.ts, frontend/src/components/layout/AppLayout.tsx, frontend/src/pages/DashboardPage.tsx, frontend/src/pages/TasksPage.tsx, frontend/src/pages/DashboardPage.test.tsx, frontend/src/test/setup.ts
- **O que fazer:** Criar projeto Vite React TS, configurar scripts (dev/build/lint/test), estrutura api/components/pages, cliente HTTP tipado com tratamento de ProblemDetail, layout com navegação, rotas Dashboard e Tarefas.
- **Pronto quando:** `npm run lint`, `npm run test` e `npm run build` passam.
- **Testes:** DashboardPage.test.tsx renderiza o título; setup com jest-dom.
- **Gate:** cd frontend && npm run lint && npm run test && npm run build
- **Commit (rascunho):** `add: Skeleton frontend Vite + React + TS`
- **Nota:** versões verificadas no npm — React 19.2.8, Vite 8.3.x, TypeScript 6.0.x, Vitest 4.1.x, React Router 7.9.x. Vitest 3.x é incompatível com Vite 8 (conflito de tipos de Plugin).

### T-F01-03 — Docker compose + infra base
- **Status:** done
- **Reqs:** RNF-04, RNF-05
- **Depende de:** T-F01-01, T-F01-02
- **Arquivos (criar/alterar):** docker-compose.yml, .env.example, backend/Dockerfile, backend/.dockerignore, frontend/Dockerfile, frontend/nginx.conf, frontend/.dockerignore
- **O que fazer:** postgres com healthcheck e volume; backend multi-stage buildando o jar; frontend multi-stage com build Vite servido por nginx fazendo proxy de /api. Validar `docker compose config`.
- **Pronto quando:** `docker compose config` é válido e os Dockerfiles constroem sem erro.
- **Testes:** nenhum teste unitário; Gate é a validação do compose e o build das imagens.
- **Gate:** docker compose config && docker compose build
- **Commit (rascunho):** `configure: Adicionar docker-compose.yml e Dockerfiles`
- **Nota:** o serviço `ollama-pull` (one-shot) já foi incluído aqui porque o backend depende dele; o restante do hardening do Ollama (instruções, RAM, GPU) entra em T-F05-01.
- **Nota 2:** o build exigiu `maven:3.9-eclipse-temurin-21-alpine` — a imagem `eclipse-temurin:21-jdk-alpine` não traz o binário `mvn`.
- **Nota 3:** o healthcheck do backend aponta para `/api/health`, criado em T-F01-04.

### T-F01-04 — Config, Flyway, migrations base, ProblemDetail, CORS
- **Status:** pending
- **Reqs:** RNF-01, RNF-20, RNF-21, ERR-02, ERR-06, TST-04
- **Depende de:** T-F01-01
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/common/error/*, backend/src/main/java/com/desafio/taskmanager/common/config/*, backend/src/main/java/com/desafio/taskmanager/common/web/*, backend/src/main/resources/db/migration/V1__schema_base.sql, backend/src/test/java/.../common/error/*, backend/src/test/resources/application-test.yml, backend/pom.xml (exclusão da tag `llm`)
- **O que fazer:** Flyway ativo com ddl-auto=validate; @RestControllerAdvice devolvendo ProblemDetail (RFC 7807) sem stack trace; CORS restrito às origens configuradas por env; mapeamento de validação (400 com lista de campos), exceção de recurso ausente (404), exceção genérica (500 seguro); smoke test de contexto com Testcontainers/Postgres.
- **Pronto quando:** migração base aplicada com sucesso em teste; respostas de erro no formato ProblemDetail e sem stack trace; CORS rejeita origem não permitida.
- **Testes:** GlobalExceptionHandlerTest (@WebMvcTest, 400/404/500 e formato), CorsTest, AiTaskManagerApplicationTests (@SpringBootTest + Testcontainers Postgres, carrega o contexto e valida o schema).
- **Gate:** mvn -q test
- **Commit (rascunho):** `add: Configuração base, Flyway, ProblemDetail e CORS`