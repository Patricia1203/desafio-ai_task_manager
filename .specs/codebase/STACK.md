# STACK.md — Versões VERIFICADAS

Verificado em 2026-10-05 consultando Maven Central (`repo1.maven.org/maven2`) e docs oficiais Spring AI 2.0.1.

| Componente | Versão | Fonte da verificação |
|---|---|---|
| Java | 21 (toolchain local: JDK 25.0.2 LTS) | `java -version` |
| Spring Boot | **4.1.1** | `spring-boot-starter-parent/4.1.1` existe no Maven Central (GA) |
| Spring AI | **2.0.1** | `spring-ai-bom/2.0.1` existe; `spring-ai-starter-model-ollama:2.0.1` declara dependências em Spring Boot 4.1.1 |
| Maven | 3.9.15 | acessado via env var `MAVEN_HOME`/PATH ou descoberta automática na máquina; caminho local não é documentado |
| Testcontainers | **2.0.5** | `org.testcontainers:testcontainers:2.0.5`; artifacts renomeados na 2.x → `testcontainers-junit-jupiter`, `testcontainers-postgresql` |
| Flyway | 12.4.0 (gerenciado pelo Boot 4.1.1) | `spring-boot-flyway:4.1.1` declara `flyway-core:12.4.0`; `flyway-database-postgresql` em runtime |
| PostgreSQL driver | gerenciado pelo Boot 4.1.1 | `org.postgresql:postgresql` (runtime) |
| Node.js | 24.15.0 | `node --version` |
| npm | 11.12.1 | `npm --version` |
| Docker Desktop | 4.84.0 / engine 29.6.2 / containerd 2.2.5 (daemon em execução) | `docker version` |

## Pontos de atenção verificados

- **Starter Web renomeado no Boot 4**: `spring-boot-starter-web` está deprecado em favor de `spring-boot-starter-webmvc` (verificado na descrição do POM 4.1.1). Usamos `spring-boot-starter-webmvc`.
- **Habilitação do chat Ollama no Spring AI 2.x**: a propriedade passou a ser `spring.ai.model.chat=ollama` (não `spring.ai.ollama.chat.enabled`, que foi removido). Verificado na doc "Ollama Chat".
- **Propriedade do modelo**: `spring.ai.ollama.chat.model` (a doc mostra `spring.ai.ollama.chat.options.model` apenas no exemplo YAML antigo; a tabela de propriedades da 2.0.1 lista `spring.ai.ollama.chat.model`).
- **Tool Calling no Ollama**: suportado pelo `OllamaChatModel`; requer Ollama >= 0.2.8 (>= 0.4.6 para streaming). Verificado na doc "Ollama Chat".
- **Structured output**: `OllamaChatOptions.outputSchema(String)` (JSON Schema) e `.format("json")` (JSON livre). `BeanOutputConverter#getJsonSchema()` gera o schema a partir do record.
- **Auto-pull de modelo**: `spring.ai.ollama.init.pull-model-strategy` (`never` por padrão).
- **Autoconfiguration do Flyway saiu do autoconfigure no Boot 4**: só adicionar `flyway-core` **não** registra o `FlywayAutoConfiguration`; é preciso o módulo `org.springframework.boot:spring-boot-flyway`. Sem ele o app sobe sem nenhuma migration aplicada e o `flyway_schema_history` nem existe (verificado em T-F01-04).
- **`@WebMvcTest` mudou de pacote no Boot 4**: agora é `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, no módulo `spring-boot-webmvc-test` (o `spring-boot-starter-test` não traz mais). Verificado em T-F01-04.
- **`PostgreSQLContainer` da Testcontainers 2.x não é mais genérico**: use `org.testcontainers.postgresql.PostgreSQLContainer` sem `<>`. O genérico ficou em `org.testcontainers.containers`. Verificado em T-F01-04.
- **`HandlerMethodValidationException#getAllValidationResults()` foi removido** no Spring 7: usar `getParameterValidationResults()` (lista de `ParameterValidationResult`).
- **`allowCredentials(false)` faz o Spring omitir** o header `Access-Control-Allow-Credentials` em vez de mandar `false`.

## Imagens Docker (tags verificadas em 2026-10-05 com `docker manifest inspect`)

| Imagem | Tag | Uso | Observação |
|---|---|---|---|
| `maven` | `3.9-eclipse-temurin-21-alpine` | build do backend | **Necessária**: a imagem `eclipse-temurin:21-jdk-alpine` **não traz o `mvn`** (build falha com `mvn: not found`). |
| `eclipse-temurin` | `21-jre-alpine` | runtime do backend | tem `wget` (busybox), usado no healthcheck do compose |
| `node` | `24-alpine` | build do frontend | bate com o Node 24.15.0 local |
| `nginx` | `1.29-alpine` | runtime do frontend | serve o build do Vite e faz proxy de `/api` |
| `postgres` | `17-alpine` | banco | tem `pg_isready` para o healthcheck |
| `ollama/ollama` | `latest` | LLM local | o binário `ollama` existe na imagem, usado no healthcheck e no pull |

Todas as imagens foram verificadas com `docker manifest inspect` e todas suportam amd64 e arm64.

## Modelo LLM

[NEEDS CLARIFICATION] Modelo exato do Ollama. Candidatos com tool calling: `qwen2.5:7b`, `llama3.1:8b`.
Decisão provisória registrada em STATE.md; confirmar em T-F03-02 / T-F04-02.