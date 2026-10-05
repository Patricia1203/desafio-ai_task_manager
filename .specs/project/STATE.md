# STATE.md

## Task atual
T-F01-03 — Docker compose + Dockerfiles — **done** (Gates: `docker compose config` e `docker compose build` verdes).
Próxima: T-F01-04 — Config, Flyway, migrations base, ProblemDetail, CORS e endpoint /api/health (Gate `mvn -q test`).

## Decisões
- 2026-10-05: Repositório já tinha commit inicial e branch `main` com remote. Não foi necessário `git init`.
- 2026-10-05: **Maven não está no PATH** desta máquina; o binário usado nos Gates está em
  `$MAVEN_HOME/bin/mvn` (Maven 3.9.15).
  Todos os Gates de backend devem invocar esse caminho até o Maven ser adicionado ao PATH.
- 2026-10-05: **Versões verificadas no Maven Central** (não estimadas): Spring Boot **4.1.1** (GA), Spring AI **2.0.1**, Testcontainers **2.0.5**.
  As versões de milestone usadas no primeiro commit (Boot 4.0.0-M3 / Spring AI 1.0.0-M4) estavam erradas e foram substituídas.
- 2026-10-05: `spring-boot-starter-web` foi substituído por `spring-boot-starter-webmvc` (starter renomeado/deprecado no Boot 4).
- 2026-10-05: Testcontainers 2.x renomeou os artifacts: `junit-jupiter` → `testcontainers-junit-jupiter`, `postgresql` → `testcontainers-postgresql`.
- 2026-10-05: `context-path: /api` definido no servidor, então os controllers ficam em `/tasks`, `/ai/tasks/...`, `/assistant/...` (URL pública `/api/tasks`).
- 2026-10-05: Docker Desktop instalado (29.6.2). O daemon estava parado durante a verificação de T-F01-01 e foi **iniciado pelo usuário** antes de T-F01-02 (Docker Desktop 4.84.0, Engine 29.6.2, containerd 2.2.5).

## Premissas [ASSUMPTION]
- 2026-10-05 (T-F01-02): Frontend scaffoldado com `npm create vite@latest -- --template react-ts` (create-vite 9.2.1).
  Versões resultantes: **React 19.2.8**, **Vite 8.3.x** (rolldown), **TypeScript 6.0.x**, **Vitest 4.1.x**, React Router 7.9.x, jsdom 27.
- 2026-10-05 (T-F01-02): O `vite.config.ts` importa `defineConfig` de `vitest/config` (e não de `vite`) para que o bloco `test` seja tipado. Importar de `vite` dá erro de tipo TS2769.
- 2026-10-05 (T-F01-02): Vitest teve de ser **4.x**, não 3.x — o Vitest 3 embute um Vite diferente e o conflito de tipos de `Plugin` quebrava o `tsc -b`.
- 2026-10-05 (T-F01-02): Bibliotecas extras no frontend, com justificativa (RNF-05):
  - `react-router-dom` — roteamento das telas Dashboard/Tarefas/Assistente (exigido por RF-20/RF-21/RF-22).
  - `vitest` + `@testing-library/react` + `@testing-library/jest-dom` + `@testing-library/user-event` + `jsdom` — base de testes exigida por TST-02 na parte de frontend.
  - `oxlint` (já vindo do template) — lint rápido sem configuração pesada.
  Nenhum UI kit, nenhuma lib de estado global, nenhuma lib de forms.
- [ASSUMPTION] O modelo padrão `qwen2.5:7b` suporta tool calling no Ollama. Não verificado (Ollama indisponível). Confirmar em T-F03-02/F-F04-02; se não suportar, trocar para `llama3.1:8b` ou implementar fallback de contexto injetado (registrar decisão).
- [ASSUMPTION] `dueDate` será `LocalDate` (data sem hora) com formato ISO-8601 `yyyy-MM-dd`. Se o desafio exigir data-hora, ajustar em F02.

- 2026-10-05 (T-F01-03): **A imagem `eclipse-temurin:21-jdk-alpine` não contém o Maven.** O build do backend falhou com `mvn: not found`. Corrigido usando `maven:3.9-eclipse-temurin-21-alpine` no estágio de build e `eclipse-temurin:21-jre-alpine` no runtime.
- 2026-10-05 (T-F01-03): Tags de imagem **verificadas** com `docker manifest inspect` antes de fixar: maven 3.9-eclipse-temurin-21-alpine, eclipse-temurin 21-jre-alpine, node 24-alpine, nginx 1.29-alpine, postgres 17-alpine, ollama/ollama latest.
- 2026-10-05 (T-F01-03): O healthcheck do backend usa `wget`, que existe na imagem JRE alpine (busybox) — verificado executando `command -v wget` no container.
- 2026-10-05 (T-F01-03): O healthcheck do backend aponta para `/api/health`, que **ainda não existe** (entra em T-F01-04). O compose só sobe o frontend depois do backend ficar healthy, então essa task precisa criar o endpoint. Registrado como dependência.
- 2026-10-05 (T-F01-03): Dockerfiles multi-stage com usuário não-root no backend; cache de Maven e de npm em camadas separadas; nginx fazendo proxy de `/api` com timeout de 300s para as chamadas de IA.

## Dúvidas [NEEDS CLARIFICATION]
- [NEEDS CLARIFICATION] `docs/desafio.pdf` não existe no repositório. Se o usuário fornecê-lo, comparar com a tabela de requisitos do prompt e registrar divergências aqui.

## Bloqueios
- Nenhum. Docker daemon em execução (verificado em T-F01-02).

## Melhorias aplicadas automaticamente
- Removido do Dockerfile do backend o `COPY` da pasta `prompts` no estágio de runtime: o `.jar` já embute `src/main/resources`, então a cópia era redundante e criava uma segunda fonte de prompts em disco.
- `.dockerignore` em `backend/` e `frontend/` para não enviar `target/`, `node_modules/` e `dist/` no contexto de build.
- Healthcheck do compose com `start_period` para dar tempo do backend subir (Flyway + JPA + Ollama na inicialização).
- Corrigido BOM UTF-8 (`\ufeff`) gerado por `Set-Content -Encoding UTF8` do PowerShell, que quebrava a compilação Java. Regra: usar a ferramenta de escrita de arquivo (sem BOM) para código Java/XML.
- `application.yml` reescrito com as propriedades corretas do Spring AI 2.x (`spring.ai.model.chat=ollama`, `spring.ai.ollama.chat.model`) e com `server.error.include-*` desabilitados para garantir que nenhuma mensagem interna vaze nas respostas.
- Adicionado `flyway-database-postgresql` (exigido pelo Flyway moderno para dialeto Postgres).
- Adicionado bloco `app.*` com CORS, timeout/retry de IA e limites de subtarefas, para que a F03 não precise mexer em contrato de config depois.

## Melhorias propostas (aguardam o usuário)
- [Proposta] Adicionar Maven Wrapper (`mvnw`) ao repositório para que os Gates funcionem em qualquer máquina sem depender do caminho local do Maven. Impacto: adicionar arquivos ao repo (baixo risco), mas muda o comando de todos os Gates de backend. Aguardando aprovação.
- [Proposta] `context-path: /api` vs. prefixo `/api` em cada controller. Mantido `context-path` (menos repetição), mas isso torna os testes `@WebMvcTest` ligeiramente diferentes. Se preferir o padrão mais explícito, é uma mudança de contrato público — requer sua decisão.

## Lições
- No PowerShell 5.1 deste ambiente, `&&` não é válido, `cat`/`head`/`tail` não existem, `npm` precisa ser chamado via `npm.cmd`, e `Set-Content -Encoding UTF8` grava BOM. Usar `;`, `Select-Object -First/-Last`, `& "C:\...\npm.cmd"` e a ferramenta de escrita de arquivos.