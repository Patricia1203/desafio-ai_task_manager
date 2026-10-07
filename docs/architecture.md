# Arquitetura — AI Task Manager

## Visão geral

A aplicação é frontend React consumindo uma API REST do backend Spring Boot. O
backend é organizado em camadas protegidas: `api` (controllers REST), `application`
(casos de uso), `domain` (regras de negócio) e `infra` (JPA/Flyway). O Spring AI
fica confinado no pacote `ai.adapter`, acessado pelas aplicações através de portas
(`TaskAiPort`, `AssistantPort`) — nenhuma camada de negócio importa Spring AI.

O Postgres e o Ollama (com o modelo `qwen2.5:7b`) rodam como serviços do Docker
Compose junto com o backend e o frontend (nginx).

```mermaid
flowchart LR
    subgraph navegador["Navegador"]
        fe["Frontend React 19 (Vite + nginx)"]
    end
    subgraph backend["Backend Spring Boot 4"]
        web["Controllers — REST /api"]
        app["Application — casos de uso"]
        dom["Domain — Task, Chat, regras"]
        port["Ports — TaskAiPort, AssistantPort"]
        adp["Adapters — Spring AI (Ollama)"]
        repo["Infra — JPA + Flyway"]
    end
    subgraph infra["Docker Compose"]
        pg[("PostgreSQL 17")]
        ol["Ollama — qwen2.5:7b"]
    end
    fe -->|"HTTP/JSON"| web
    web --> app
    app --> dom
    app --> repo
    repo --> pg
    app --> port
    port --> adp
    adp -->|"Ollama API"| ol
    pg -.->|"schema via Flyway"| repo
```

## Fluxo de uma chamada de IA (análise de tarefa)

Exemplo do endpoint `POST /api/ai/tasks/{id}/analyze`: o adapter monta o prompt
com o schema do structured output (via `ChatClient.responseEntity(Class)`), o
modelo responde JSON e o `LlmResponseValidator` valida sempre; resposta inválida
dispara retry com instrução de correção; esgotado, vira
`InvalidLlmResponseException` (502).

```mermaid
sequenceDiagram
    participant U as Usuario / UI
    participant C as AiTaskController
    participant S as AiTaskService
    participant A as SpringAiTaskAiAdapter
    participant L as LlmResponseValidator
    participant O as Ollama (qwen2.5:7b)
    U->>C: POST /api/ai/tasks/{id}/analyze
    C->>S: analyze(id)
    S->>S: findById — 404 se a tarefa nao existe (ERR-01)
    S->>A: analyze(TaskAiContext)
    loop até app.ai.max-retries + 1 tentativas
        A->>O: prompt + JSON Schema (structured output)
        O-->>A: resposta JSON
        A->>L: valida enums, limites e campos
        alt resposta invalida e resta tentativa
            A->>O: correcao como SystemMessage
        else resposta invalida, tentativas esgotadas
            A-->>S: InvalidLlmResponseException → 502 (ERR-04)
        end
    end
    S-->>C: TaskAnalysis (JSON em portugues)
    C-->>U: 200
```

## Fluxo do assistente com tool calling

`POST /api/assistant/chat`: o assistente usa ferramentas somente-leitura
(`get_pending_tasks`, `get_overdue_tasks`, `get_task_by_id`,
`get_tasks_by_priority`, `get_tasks_due_soon`, `get_task_summary`) para responder
com dados reais. O turno (USER + ASSISTANT) só é persistido depois que a IA
responde.

```mermaid
sequenceDiagram
    participant U as Usuario
    participant S as AssistantService
    participant A as SpringAiAssistantAdapter
    participant O as Ollama (qwen2.5:7b)
    participant T as TaskQueryTools (somente-leitura)
    U->>S: POST /api/assistant/chat (conversationId?, mensagem)
    S->>S: resolve conversa (cria ou retoma) + historico (janela de 20)
    S->>A: chat(historico, mensagem)
    A->>O: prompt + 6 ferramentas registradas
    O-->>A: tool_calls (ex.: get_overdue_tasks)
    A->>T: executa a ferramenta
    T-->>A: resultado (limitado por app.assistant.max-tool-results)
    A->>O: proxima chamada com os resultados (grounding)
    O-->>A: resposta final em linguagem natural
    A-->>S: resposta
    S->>S: persiste USER + ASSISTANT
    S-->>U: ChatResponse (conversationId, resposta)
```

## Camadas protegidas (RNF-14)

O `LayerDependenciesTest` falha se `task.domain`, `task.application`, `ai.port`,
`assistant.port` ou `assistant/application` importarem `org.springframework.ai`.
O pacote `ai.adapter` concentra todo o Spring AI (`SpringAiTaskAiAdapter`,
`SpringAiAssistantAdapter`, `AssistantToolCallbacks`, `AiAdapterConfig`).