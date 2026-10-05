# tasks.md — F01-foundation

### T-F01-01 — Bootstrap backend (Spring Boot 4, Java 21)
- Status: done
- Reqs: RNF-01, RNF-20
- Depende de: (nenhuma)
- Arquivos (criar/alterar): backend/, backend/pom.xml, backend/src/main/java/.../Application.java, backend/src/main/resources/application.yml, backend/src/main/resources/application-dev.yml
- O que fazer: Inicializar projeto Maven com dependências Spring Boot 4, Web, Data JPA, Validation, PostgreSQL, Flyway (verificar versões antes). Configurar Java 21.
- Pronto quando: Compila (mvn compile) sem erros; estrutura pacotes por feature criada.
- Testes: TST-04 smoke mínimo (context loads opcional com Testcontainers configurado depois).
- Gate: cd backend && mvn -q compile
- Commit (rascunho): dd: Bootstrap backend Spring Boot 4 (Java 21)

### T-F01-02 — Skeleton frontend (Vite + React + TS)
- Status: pending
- Reqs: RNF-03
- Depende de: T-F01-01
- Arquivos: frontend/
- O que fazer: Criar Vite React TS, configurar scripts, estrutura api/components/pages.
- Pronto quando: 
pm run build passa.
- Testes: teste placeholder se desejado (Vitest configurado depois).
- Gate: cd frontend && npm run build
- Commit: dd: Skeleton frontend Vite + React + TS

### T-F01-03 — Docker compose + infra base
- Status: pending
- Reqs: RNF-04
- Depende de: T-F01-01, T-F01-02
- Arquivos: docker-compose.yml, .env.example, backend/Dockerfile, frontend/Dockerfile
- O que fazer: postgres (healthcheck/volume), ollama (preparado), backend, frontend; validar docker compose config.
- Pronto quando: docker compose config válido.
- Testes: nenhum teste unitário.
- Gate: docker compose config
- Commit: configure: Adicionar docker-compose.yml e Dockerfiles

### T-F01-04 — Config, Flyway, ProblemDetail, CORS
- Status: pending
- Reqs: RNF-01, RNF-21, RNF-20
- Depende de: T-F01-01
- Arquivos: common/error/*, infra/config/*, resources/db/migration/V1__schema_base.sql, application.yml
- O que fazer: Flyway ativo, ddl-auto=validate, @RestControllerAdvice ProblemDetail sem stack trace, CORS restrito por env.
- Pronto quando: Migração válida, erros mapeados, config lê env.
- Testes: TST-04/webmvc testes de erros.
- Gate: cd backend && mvn -q test -Dtest=*Error*Test* 2>&1 | tail -5
- Commit: dd: Configuração base, Flyway, ProblemDetail e CORS
