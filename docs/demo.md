# Roteiro de demonstração — AI Task Manager

Roteiro para apresentar o fluxo completo sem consultar o código. Cada etapa tem o
comando ou o clique na UI. Total estimado: 20–25 minutos.

## 0. Subir a stack

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:8081
- API: http://localhost:8080/api (healthcheck `GET /api/health` devolve `status: UP`)
- A primeira subida baixa o `qwen2.5:7b` (~4,7 GB) pelo serviço one-shot
  `ollama-pull`; o backend só sobe depois que o modelo fica pronto.

> Sem Docker: backend com `mvn spring-boot:run` (Ollama/Postgres acessíveis), frontend
> com `npm run dev` (origem `http://localhost:5173` já liberada no CORS).

## 1. Tarefas (CRUD + subtarefas) — 5 min

1. No Dashboard, conferir os indicadores vazios.
2. Criar tarefa pela UI (formulário) ou:

```bash
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Preparar pauta da reunião","descricao":"Levantar tópicos e prazos","prioridade":"ALTA","prazo":"2026-10-31"}'
```

3. Listar com filtro `GET /api/tasks?status=A_FAZER`; abrir o detalhe.
4. Alterar status para `EM_ANDAMENTO` (PATCH) e criar uma subtarefa
   `POST /api/tasks/{id}/subtasks` (ou pela UI). Excluir o pai e observar a
   confirmação listando as subtarefas (cascata).

## 2. IA: melhorar, analisar e decompor — 7 min

No detalhe da tarefa, o painel de IA (botões Melhorar, Analisar, Dividir):

- **Melhorar** → reescrita do título/descrição (sugestão; nada é gravado). Aplicar
  atualiza a tarefa preservando prioridade e prazo.
- **Analisar** → prioridade, complexidade, horas estimadas e justificativa — a
  tarefa **não** é alterada.
- **Dividir** → sugestão de subtarefas; marcar algumas e "Adicionar como tarefas"
  cria as selecionadas sob o pai.

Equivalente por API:

```bash
curl -s -X POST http://localhost:8080/api/ai/tasks/{id}/improve   -H "Content-Type: application/json" -d '{}'
curl -s -X POST http://localhost:8080/api/ai/tasks/{id}/analyze
curl -s -X POST http://localhost:8080/api/ai/tasks/{id}/decompose
curl -s -X POST http://localhost:8080/api/ai/tasks/{id}/decompose/apply \
  -H "Content-Type: application/json" \
  -d '{"subtarefas":[{"titulo":"Entrevistar stakeholders","horasEstimadas":2}]}'
```

## 3. Assistente com tool calling — 5 min

1. Abrir **Assistente** e perguntar: `Quais tarefas estão pendentes?` → resposta
   lista tarefas reais (ferramenta `get_pending_tasks`).
2. `Quais tarefas vencem em 7 dias?` → `get_tasks_due_soon`.
3. Enviar uma segunda mensagem e observar a conversa retomada (mesmo
   `conversationId`); "Nova conversa" zera o histórico na tela.
4. Fora de escopo: `Me explique o teorema de Pitágoras` → recusa educada
   (o prompt de sistema manda responder apenas sobre tarefas).

Por API:

```bash
curl -s -X POST http://localhost:8080/api/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"mensagem":"Quais tarefas estão pendentes?"}'
```

## 4. Erros e resiliência — 5 min

- **403/404:** `GET /api/tasks/{uuid-aleatorio}` → `404` ProblemDetail sem stack trace.
- **400:** `POST /api/tasks` sem `titulo` → `400` com `errors[].field: titulo`.
- **422:** trocar `CONCLUIDA → EM_ANDAMENTO` → `422` (reabrir exige `A_FAZER`).
- **503:** `docker compose stop ollama` → chamar `POST /api/ai/tasks/{id}/analyze`
  → `503` com `traceId` e `timestamp` no corpo; o mesmo id aparece no header
  `X-Trace-Id`. Subir de novo com `docker compose start ollama`.
- **500:** com o Postgres parado, `GET /api/health` → `503`; nenhuma resposta
  contém stack trace, SQL ou nome de classe.

Todo envelope de erro é RFC 7807 (`type/status/title/detail`) sem detalhe interno;
o id no corpo/header cruza com a linha de log correspondente.

## 5. Pontos técnicos para a apresentação — 5 min

- **Arquitetura:** backend em camadas (`api → application → domain → infra`), o
  Spring AI confinado em `ai.adapter` e as aplicações falando só com portas
  (`TaskAiPort`, `AssistantPort`) — trocar de provider mexe num único lugar.
  Diagrama em [`docs/architecture.md`](architecture.md).
- **Structured output:** `ChatClient.responseEntity(Class)` (o JSON Schema chega no
  prompt), validação **sempre** pelo `LlmResponseValidator` e retry com a correção
  como `SystemMessage`; esgotado → `InvalidLlmResponseException` → 502
  `code: LLM_INVALID_RESPONSE`.
- **Escolha do modelo:** `qwen2.5:7b` (verificado com tool calling real em T-F05-01,
  ~4,7 GB, roda sem GPU). Alternativa `llama3.1:8b`; se um modelo não suportar tool
  calling, o toggle `app.assistant.tool-calling=false` injeta um contexto pré-montado
  no prompt como fallback.
- **Contexto enxuto (RNF-11/RNF-13):** só `title`/`description`/`priority` sobem ao
  modelo (truncados nos limites), ferramentas do assistente são somente-leitura e
  limitadas por `max-tool-results`, e o histórico usa janela de 20 mensagens.
- **Erros:** mapa completo 400/404/422/500/502/503 com `traceId`/`timestamp`,
  `detail` fixo e causa só no log (T-F05-02 revisou os seis cenários ponta a ponta).
- **Banco:** Flyway (`V1` convenções, `V2` tasks, `V3` chat) com índices e enums
  como `VARCHAR + CHECK`; testes de integração sobre Postgres real via Testcontainers
  (container compartilhado mantém a suíte em ~1 min).
- **Testes:** backend `mvn -q test` = **227 testes / 0 falhas** (modelo de IA é um
  `ChatModel` falso roteirizado — sem LLM real no build; portas de IA cobertas por
  fakes). Frontend `npm run lint && npm run test && npm run build` = **31 testes**.