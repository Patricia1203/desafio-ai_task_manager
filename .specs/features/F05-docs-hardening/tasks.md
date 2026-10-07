# tasks.md — F05-docs-hardening

### T-F05-01 — Compose com Ollama e pull automático do modelo
- **Status:** done (commit `9a04224`, 2026-10-07)
- **Reqs:** RNF-04, RNF-05
- **Depende de:** T-F01-03, T-F03-02
- **Arquivos (criar/alterar):** docker-compose.yml, scripts/ollama-pull.sh, .env.example
- **O que fazer:** Adicionar serviço `ollama` com volume persistente e healthcheck; serviço one-shot que executa `ollama pull $AI_MODEL` e só então libera o backend; backend passa a apontar para `http://ollama:11434`. Documentar no `.env.example` como apontar para um Ollama rodando fora do Docker via `host.docker.internal`.
- **Pronto quando:** `docker compose config` é válido; o serviço one-shot de pull termina com sucesso com o modelo já disponível; documentado o caminho para Ollama externo.
- **Testes:** nenhum teste unitário; Gate é a validação do compose e a execução do serviço de pull.
- **Gate:** docker compose config && docker compose run --rm ollama-pull
- **Commit (rascunho):** `configure: Adicionar Ollama ao compose com pull automático do modelo`
- **Evidências:** o compose (serviços `ollama` + one-shot `ollama-pull` + backend apontando para `http://ollama:11434`) já existia desde T-F01-03 (`b83deef`); esta task criou `scripts/ollama-pull.sh` (pull do `AI_MODEL` contra `OLLAMA_HOST`, para o backend fora do Docker) e executou o pull de verdade pela primeira vez: `docker compose config` válido e `docker compose run --rm ollama-pull` termina com `qwen2.5:7b` (4.7 GB) no volume `ollama-data`, serviço healthy. Premissa de T-F04-03 resolvida: smoke em `/api/chat` com a tool `criar_tarefa` devolveu `message.tool_calls` com argumentos válidos (`titulo` "Preparar pauta", `prioridade` "alta") — `qwen2.5:7b` suporta tool calling de verdade; o toggle `app.assistant.tool-calling` permanece ligado e nenhum fallback (Grok/contexto) é necessário. `.env.example` já documentava o Ollama externo via `host.docker.internal` desde T-F01-03 — sem alteração. Container de teste parado após o gate (`docker compose stop ollama`).

### T-F05-02 — Revisão dos erros ERR-01 a ERR-06 ponta a ponta
- **Status:** pending
- **Reqs:** ERR-01, ERR-02, ERR-03, ERR-04, ERR-05, ERR-06
- **Depende de:** T-F02-04, T-F03-03, T-F04-03
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/common/error/GlobalExceptionHandler.java, backend/src/test/java/.../common/error/*, .specs/project/TRACEABILITY.md
- **O que fazer:** Conferir cada cenário do desafio contra a implementação: 404 para tarefa inexistente, 400 com lista de campos para dados inválidos, 502 para erro de comunicação com o LLM, 502 com código LLM_INVALID_RESPONSE para resposta inválida, 503 para Ollama indisponível e 500 genérico para erro de persistência. Nenhuma resposta pode conter stack trace; a causa é logada no servidor. traceId e timestamp ja existem desde T-F02-05k (`TraceIdFilter` + propriedades do ProblemDetail): aqui so falta verificar que os cenarios de IA (502/503) herdam os dois.
- **Pronto quando:** cada cenário tem teste automatizado que verifica status, formato ProblemDetail e ausência de stack trace.
- **Testes:** suíte de testes de erro cobrindo ERR-01 a ERR-06 ponta a ponta (slice web para 400/404/500, integração com porta de IA fake para 502/503).
- **Gate:** mvn -q test
- **Commit (rascunho):** `fix: Revisar tratamento de erros ERR-01 a ERR-06`

### T-F05-03 — README completo com 7 seções e diagrama
- **Status:** pending
- **Reqs:** DOC-01, RNF-05, RNF-10, DEL-02
- **Depende de:** T-F05-01, T-F05-02
- **Arquivos (criar/alterar):** README.md, docs/architecture.md
- **O que fazer:** Escrever as 7 seções do DOC-01: Descrição, Tecnologias, Arquitetura (com diagrama), Configuração do LLM (modelo, provider, config, como rodar), Execução (backend, frontend, banco, LLM), Recursos de IA (prompts, funcionalidades, processamento das respostas, Tool Calling) e Decisões técnicas. Justificar toda biblioteca extra. Incluir diagrama de arquitetura e o fluxo de uma chamada de IA.
- **Pronto quando:** as 7 seções existem com o conteúdo exigido; o diagrama é renderizável; toda dependência extra está justificada.
- **Testes:** nenhum teste automatizado; Gate é a checagem de que as 7 seções existem e que nenhum marcador de preenchimento restou.
- **Gate:** rg -c "^## " README.md e ausência de "TODO" no README
- **Commit (rascunho):** `document: Completar README com 7 seções, diagrama e decisões técnicas`

### T-F05-04 — Rastreabilidade final e roteiro de demonstração
- **Status:** pending
- **Reqs:** DEL-01, DEL-02
- **Depende de:** T-F05-03
- **Arquivos (criar/alterar):** .specs/project/TRACEABILITY.md, .specs/project/STATE.md, docs/demo.md
- **O que fazer:** Preencher a matriz de rastreabilidade com task, código, testes e hash de commit de cada REQ; nenhum REQ [OBR] pode ficar sem task, sem teste ou fora de `done`. Escrever o roteiro de apresentação cobrindo arquitetura, Spring AI, escolha do modelo, prompts, structured output, contexto, erros, separação IA/negócio, banco e testes. Registrar as lições aprendidas em STATE.md.
- **Pronto quando:** todos os REQs [OBR] da seção 1 do desafio estão com status `done` e evidência; o roteiro permite executar o fluxo completo sem consultar o código.
- **Testes:** nenhum teste automatizado; Gate é a checagem de que nenhuma linha da matriz está com status diferente de `done` para REQ [OBR].
- **Gate:** verificação da matriz de rastreabilidade
- **Commit (rascunho):** `document: Finalizar rastreabilidade e roteiro de demonstração`