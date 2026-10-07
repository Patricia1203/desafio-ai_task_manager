# tasks.md — F05-docs-hardening

### T-F05-01 — Compose com Ollama e pull automático do modelo
- **Status:** done (commit `f7bd318`, 2026-10-07)
- **Reqs:** RNF-04, RNF-05
- **Depende de:** T-F01-03, T-F03-02
- **Arquivos (criar/alterar):** docker-compose.yml, scripts/ollama-pull.sh, .env.example
- **O que fazer:** Adicionar serviço `ollama` com volume persistente e healthcheck; serviço one-shot que executa `ollama pull $AI_MODEL` e só então libera o backend; backend passa a apontar para `http://ollama:11434`. Documentar no `.env.example` como apontar para um Ollama rodando fora do Docker via `host.docker.internal`.
- **Pronto quando:** `docker compose config` é válido; o serviço one-shot de pull termina com sucesso com o modelo já disponível; documentado o caminho para Ollama externo.
- **Testes:** nenhum teste unitário; Gate é a validação do compose e a execução do serviço de pull.
- **Gate:** docker compose config && docker compose run --rm ollama-pull
- **Commit (rascunho):** `configure: Adicionar Ollama ao compose com pull automático do modelo`
- **Evidências:** o compose (serviços `ollama` + one-shot `ollama-pull` + backend apontando para `http://ollama:11434`) já existia desde T-F01-03 (`c97b2bb`); esta task criou `scripts/ollama-pull.sh` (pull do `AI_MODEL` contra `OLLAMA_HOST`, para o backend fora do Docker) e executou o pull de verdade pela primeira vez: `docker compose config` válido e `docker compose run --rm ollama-pull` termina com `qwen2.5:7b` (4.7 GB) no volume `ollama-data`, serviço healthy. Premissa de T-F04-03 resolvida: smoke em `/api/chat` com a tool `criar_tarefa` devolveu `message.tool_calls` com argumentos válidos (`titulo` "Preparar pauta", `prioridade` "alta") — `qwen2.5:7b` suporta tool calling de verdade; o toggle `app.assistant.tool-calling` permanece ligado e nenhum fallback (Grok/contexto) é necessário. `.env.example` já documentava o Ollama externo via `host.docker.internal` desde T-F01-03 — sem alteração. Container de teste parado após o gate (`docker compose stop ollama`).

### T-F05-02 — Revisão dos erros ERR-01 a ERR-06 ponta a ponta
- **Status:** done (commit `bc68a2e`, 2026-10-07)
- **Reqs:** ERR-01, ERR-02, ERR-03, ERR-04, ERR-05, ERR-06
- **Depende de:** T-F02-04, T-F03-03, T-F04-03
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/common/error/GlobalExceptionHandler.java, backend/src/test/java/.../common/error/*, .specs/project/TRACEABILITY.md
- **O que fazer:** Conferir cada cenário do desafio contra a implementação: 404 para tarefa inexistente, 400 com lista de campos para dados inválidos, 502 para erro de comunicação com o LLM, 502 com código LLM_INVALID_RESPONSE para resposta inválida, 503 para Ollama indisponível e 500 genérico para erro de persistência. Nenhuma resposta pode conter stack trace; a causa é logada no servidor. traceId e timestamp ja existem desde T-F02-05k (`TraceIdFilter` + propriedades do ProblemDetail): aqui so falta verificar que os cenarios de IA (502/503) herdam os dois.
- **Pronto quando:** cada cenário tem teste automatizado que verifica status, formato ProblemDetail e ausência de stack trace.
- **Testes:** suíte de testes de erro cobrindo ERR-01 a ERR-06 ponta a ponta (slice web para 400/404/500, integração com porta de IA fake para 502/503).
- **Gate:** mvn -q test
- **Commit (rascunho):** `fix: Revisar tratamento de erros ERR-01 a ERR-06`
- **Evidências:** revisão confirmou que o `GlobalExceptionHandler` já cobre ERR-01 a ERR-06 (404, 400 com `errors[].field`, 502 de comunicação, 502 `code: LLM_INVALID_RESPONSE`, 503 de indisponibilidade, 500 genérico/persistência) sem vazar stack trace, herdando `traceId`/`timestamp` desde T-F02-05k. Lacunas encontradas e corrigidas — mudanças só em teste: o slice não tinha probe para ERR-04 (criado `/__test/llm-invalida` no `ErrorProbeController`); os testes 502/503 não afirmavam `traceId` (16 hex), `timestamp` ISO-8601 e ausência de stack trace no corpo (asserts adicionados em `GlobalExceptionHandlerTest`, `AssistantControllerTest` e `AiTaskControllerTest`, com `@Import(TraceIdFilter.class)`); `AssistantServiceTest` substituiu o RuntimeException genérico por 3 testes de propagação tipada (`LlmCommunicationException`, `LlmUnavailableException`, `InvalidLlmResponseException`) com `.isSameAs(erro)` + `verify(mensagens, never()).save` — o teste antigo passava por acaso (a exceção lançada era `ResourceNotFoundException`, também `RuntimeException`, pois o stub usava `UUID.randomUUID()` diferente do id da chamada). Gate `mvn -q test`: **227 testes / 0 falhas**.

### T-F05-03 — README completo com 7 seções e diagrama
- **Status:** done (commit `85afdaa`, 2026-10-07)
- **Reqs:** DOC-01, RNF-05, RNF-10, DEL-02
- **Depende de:** T-F05-01, T-F05-02
- **Arquivos (criar/alterar):** README.md, docs/architecture.md
- **O que fazer:** Escrever as 7 seções do DOC-01: Descrição, Tecnologias, Arquitetura (com diagrama), Configuração do LLM (modelo, provider, config, como rodar), Execução (backend, frontend, banco, LLM), Recursos de IA (prompts, funcionalidades, processamento das respostas, Tool Calling) e Decisões técnicas. Justificar toda biblioteca extra. Incluir diagrama de arquitetura e o fluxo de uma chamada de IA.
- **Pronto quando:** as 7 seções existem com o conteúdo exigido; o diagrama é renderizável; toda dependência extra está justificada.
- **Testes:** nenhum teste automatizado; Gate é a checagem de que as 7 seções existem e que nenhum marcador de preenchimento restou.
- **Gate:** rg -c "^## " README.md e ausência de "TODO" no README
- **Commit (rascunho):** `document: Completar README com 7 seções, diagrama e decisões técnicas`
- **Evidências:** README.md reescrito com as 7 seções de `^## ` (Descrição, Tecnologias, Arquitetura, Configuração do LLM, Execução, Recursos de IA, Decisões técnicas) — `grep -c "^## "` = 7 e nenhum marcador `TODO` restou (a palavra "todo" em português foi evitada numa frase para não falso-positivar um check case-insensitive). `docs/architecture.md` traz o diagrama renderizável (mermaid) de componentes + o fluxo de uma chamada de IA (`analyze`, com retry/validação) + o fluxo do assistente com tool calling, e as camadas protegidas (RNF-14). Decisões técnicas justificam cada biblioteca extra: `spring-ai-starter-model-ollama` (única dep de IA, atrás das portas), `react-router-dom` (rotas RF-20/21/22), `vitest` + Testing Library + jsdom (TST-02), `oxlint`, `spring-boot-flyway` + `flyway-database-postgresql` (Boot 4 modular), `spring-boot-webmvc-test` (`@WebMvcTest` no Boot 4) e Testcontainers (Postgres real). Configuração do LLM documenta `OLLAMA_BASE_URL`/`AI_MODEL`/`AI_TIMEOUT`/`AI_MAX_RETRIES`/`ASSISTANT_TOOL_CALLING`, o one-shot `ollama-pull` e o `scripts/ollama-pull.sh`; Execução cobre compose, sem Docker e testes.

### T-F05-04 — Rastreabilidade final e roteiro de demonstração
- **Status:** done
- **Reqs:** DEL-01, DEL-02
- **Depende de:** T-F05-03
- **Arquivos (criar/alterar):** .specs/project/TRACEABILITY.md, .specs/project/STATE.md, docs/demo.md
- **O que fazer:** Preencher a matriz de rastreabilidade com task, código, testes e hash de commit de cada REQ; nenhum REQ [OBR] pode ficar sem task, sem teste ou fora de `done`. Escrever o roteiro de apresentação cobrindo arquitetura, Spring AI, escolha do modelo, prompts, structured output, contexto, erros, separação IA/negócio, banco e testes. Registrar as lições aprendidas em STATE.md.
- **Pronto quando:** todos os REQs [OBR] da seção 1 do desafio estão com status `done` e evidência; o roteiro permite executar o fluxo completo sem consultar o código.
- **Testes:** nenhum teste automatizado; Gate é a checagem de que nenhuma linha da matriz está com status diferente de `done` para REQ [OBR].
- **Gate:** verificação da matriz de rastreabilidade
- **Commit (rascunho):** `document: Finalizar rastreabilidade e roteiro de demonstração`
- **Commit:** `67698fa` (`document: Finalizar rastreabilidade e roteiro de demonstração`)
- **Evidências:** matriz rastreável fechada — **0 linhas `pending`**; cada REQ [OBR] (RF-01..RF-24, RNF-01..RNF-21, ERR-01..ERR-06, TST-01..TST-04, DOC-01, DEL-01, DEL-02) tem task, código, testes e hash de commit; os placeholders `(T-...)` foram substituídos pelos hashes reais extraídos do `git log` (a trailer ` - Task: T-...` de cada commit é a âncora do mapeamento). DEL-01 (estrutura `backend/`, `frontend/`, `README.md`, `docker-compose.yml`, `docs/`) e DEL-02 (roteiro `docs/demo.md` passo a passo cobrindo stack, CRUD, IA melhorar/analisar/decompor, assistente com tool calling, erros 400/404/422/500/502/503 e pontos técnicos de apresentação) preenchidos. Linha `TST-02` duplicada na matriz corrigida (uma edit corrompeu a linha `TST-03` vizinha e foi reparada no diff). Lições registradas no STATE.md.