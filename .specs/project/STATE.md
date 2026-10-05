# STATE.md

## Task atual
T-F01-01 — Bootstrap backend (Spring Boot 4, Java 21) — **in-progress** (Gate `mvn compile` verde; falta registrar testes base na F01-04).

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
- 2026-10-05: Docker Desktop instalado (29.6.2) mas o **daemon estava parado** durante a verificação. Testcontainers e `docker compose` exigem o daemon em execução.

## Premissas [ASSUMPTION]
- [ASSUMPTION] O modelo padrão `qwen2.5:7b` suporta tool calling no Ollama. Não verificado (Ollama indisponível). Confirmar em T-F03-02/F-F04-02; se não suportar, trocar para `llama3.1:8b` ou implementar fallback de contexto injetado (registrar decisão).
- [ASSUMPTION] `dueDate` será `LocalDate` (data sem hora) com formato ISO-8601 `yyyy-MM-dd`. Se o desafio exigir data-hora, ajustar em F02.

## Dúvidas [NEEDS CLARIFICATION]
- [NEEDS CLARIFICATION] Tags exatas de `eclipse-temurin` e `node` para os Dockerfiles (verificar no Docker Hub em T-F01-03).
- [NEEDS CLARIFICATION] `docs/desafio.pdf` não existe no repositório. Se o usuário fornecê-lo, comparar com a tabela de requisitos do prompt e registrar divergências aqui.

## Bloqueios
- Nenhum bloqueio de código.
- Ambiente: Docker daemon parado → qualquer Gate que dependa de Testcontainers ou `docker compose up` precisa do Docker Desktop iniciado.

## Melhorias aplicadas automaticamente
- Corrigido BOM UTF-8 (`\ufeff`) gerado por `Set-Content -Encoding UTF8` do PowerShell, que quebrava a compilação Java. Regra: usar a ferramenta de escrita de arquivo (sem BOM) para código Java/XML.
- `application.yml` reescrito com as propriedades corretas do Spring AI 2.x (`spring.ai.model.chat=ollama`, `spring.ai.ollama.chat.model`) e com `server.error.include-*` desabilitados para garantir que nenhuma mensagem interna vaze nas respostas.
- Adicionado `flyway-database-postgresql` (exigido pelo Flyway moderno para dialeto Postgres).
- Adicionado bloco `app.*` com CORS, timeout/retry de IA e limites de subtarefas, para que a F03 não precise mexer em contrato de config depois.

## Melhorias propostas (aguardam o usuário)
- [Proposta] Adicionar Maven Wrapper (`mvnw`) ao repositório para que os Gates funcionem em qualquer máquina sem depender do caminho local do Maven. Impacto: adicionar arquivos ao repo (baixo risco), mas muda o comando de todos os Gates de backend. Aguardando aprovação.
- [Proposta] `context-path: /api` vs. prefixo `/api` em cada controller. Mantido `context-path` (menos repetição), mas isso torna os testes `@WebMvcTest` ligeiramente diferentes. Se preferir o padrão mais explícito, é uma mudança de contrato público — requer sua decisão.

## Lições
- No PowerShell 5.1 deste ambiente, `&&` não é válido, `cat`/`head`/`tail` não existem, `npm` precisa ser chamado via `npm.cmd`, e `Set-Content -Encoding UTF8` grava BOM. Usar `;`, `Select-Object -First/-Last`, `& "C:\...\npm.cmd"` e a ferramenta de escrita de arquivos.