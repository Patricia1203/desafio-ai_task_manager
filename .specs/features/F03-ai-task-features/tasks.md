# tasks.md — F03-ai-task-features

### T-F03-01 — Porta de IA, DTOs estruturados e prompts
- **Status:** done
- **Reqs:** RF-10, RF-11, RF-12, RNF-10, RNF-11, RNF-14
- **Depende de:** T-F02-04
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/ai/port/TaskAiPort.java, backend/src/main/java/com/desafio/taskmanager/ai/port/dto/TaskImprovement.java, TaskAnalysis.java, TaskComplexity.java, TaskDecomposition.java, ProposedSubtask.java, TaskAiContext.java, backend/src/main/java/com/desafio/taskmanager/ai/application/LlmResponseValidator.java, backend/src/main/resources/prompts/task-improve.st, task-analyze.st, task-decompose.st, backend/src/main/java/com/desafio/taskmanager/common/error/InvalidLlmResponseException.java, backend/src/test/java/.../ai/application/LlmResponseValidatorTest.java
- **O que fazer:** Definir a porta `TaskAiPort` na camada de aplicação sem nenhum import de `org.springframework.ai`; DTOs como `record` com `@JsonProperty` para gerar o JSON Schema; prompts em arquivos `.st` versionados e parametrizados (um por caso de uso, com papel, regras, formato de saída e proibição de inventar); validador pós-LLM com os limites de configuração.
- **Pronto quando:** a porta compila sem Spring AI no classpath de domínio; o validador rejeita enums inválidos, estimatedHours fora do intervalo, textos vazios ou acima do tamanho máximo e quantidade de subtarefas fora de 2..10; a arquitetura é protegida por teste.
- **Testes:** LlmResponseValidatorTest (caso válido, enum inválido, hours fora do intervalo, subtarefas duplicadas/vazias/excedentes) e teste de arquitetura que falha se `task.domain`, `task.application` ou `ai.port` importarem `org.springframework.ai`.
- **Gate:** mvn -q test -Dtest=LlmResponseValidatorTest
- **Commit (rascunho):** `add: Porta de IA, DTOs estruturados e prompts versionados`

### T-F03-02 — Adaptador Spring AI com structured output e retry
- **Status:** done
- **Reqs:** RNF-02, RNF-10, RNF-11, RNF-13, RNF-14, ERR-03, ERR-04, ERR-05, TST-03
- **Depende de:** T-F03-01
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/ai/adapter/SpringAiTaskAiAdapter.java, backend/src/main/java/com/desafio/taskmanager/ai/adapter/config/AiProperties.java, backend/src/main/java/com/desafio/taskmanager/ai/adapter/config/AiAdapterConfig.java, backend/src/main/java/com/desafio/taskmanager/common/error/LlmUnavailableException.java, LlmCommunicationException.java, backend/src/test/java/.../ai/adapter/SpringAiTaskAiAdapterTest.java, backend/src/test/java/.../ai/adapter/FakeChatModelSupport.java, backend/pom.xml (excluir tag `llm`)
- **O que fazer:** Implementar a porta com `ChatClient`/`ChatModel` do Spring AI usando structured output (`entity(...)` com o schema do record); validar sempre a resposta; um retry controlado com instrução de correção em caso de parse/validação inválida; mapear timeout/conexão para LlmCommunicationException e Ollama fora do ar para LlmUnavailableException; enviar ao modelo somente os campos necessários, com truncamento.
- **Pronto quando:** caso feliz devolve o record validado; JSON inválido, enum inválido e campo faltante disparam um retry e depois InvalidLlmResponseException; timeout e conexão recusada viram as exceções mapeadas; nenhum campo além do contextoallowed chega ao prompt.
- **Testes:** SpringAiTaskAiAdapterTest com `ChatModel` mockado: caso feliz, JSON inválido (retry + erro), enum inválido, campo faltando, exceção de comunicação, timeout e LLM indisponível.
- **Gate:** mvn -q test -Dtest=SpringAiTaskAiAdapterTest
- **Commit (rascunho):** `add: Adaptador Spring AI com structured output e retry`

### T-F03-03 — Serviço e endpoints de IA para tarefas
- **Status:** pending
- **Reqs:** RF-10, RF-11, RF-12, RF-13, RF-14, RF-24, RNF-12, RNF-15, ERR-01, ERR-04
- **Depende de:** T-F03-02
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/task/application/AiTaskService.java, backend/src/main/java/com/desafio/taskmanager/ai/api/AiTaskController.java, backend/src/main/java/com/desafio/taskmanager/ai/api/dto/ImproveTaskRequest.java, AnalyzeTaskResponse.java, DecomposeTaskResponse.java, ApplyDecompositionRequest.java, SubtaskDraft.java, backend/src/test/java/.../task/application/AiTaskServiceTest.java, backend/src/test/java/.../ai/api/AiTaskControllerTest.java
- **O que fazer:** improve e decompose retornam apenas sugestão e não persistem nada; analyze devolve a análise tipada e nunca altera a tarefa automaticamente; decompose/apply recebe as subtarefas aceitas pelo usuário, valida cada uma e cria tarefas com parentTaskId apontando para a original.
- **Pronto quando:** nenhum endpoint de sugestão grava no banco; apply cria exatamente as subtarefas enviadas e rejeita drafts inválidos com 400; tarefa inexistente devolve 404; resposta de LLM inválida devolve 502 com código LLM_INVALID_RESPONSE.
- **Testes:** AiTaskServiceTest (improve/decompose não persistem, analyze não altera prioridade, apply cria subtarefas e valida drafts) com a porta fake; AiTaskControllerTest (200/400/404/502 e formato ProblemDetail).
- **Gate:** mvn -q test -Dtest=AiTaskServiceTest+AiTaskControllerTest
- **Commit (rascunho):** `add: Serviço e endpoints de IA para tarefas`

### T-F03-04 — Painel de IA no frontend
- **Status:** pending
- **Reqs:** RF-10, RF-11, RF-12, RF-13, RF-14, RF-21, RNF-15
- **Depende de:** T-F03-03
- **Arquivos (criar/alterar):** frontend/src/api/ai.ts, frontend/src/types/ai.ts, frontend/src/components/task/AiPanel.tsx, frontend/src/components/task/ImproveResult.tsx, frontend/src/components/task/AnalysisResult.tsx, frontend/src/components/task/DecompositionResult.tsx, frontend/src/pages/TasksPage.tsx, frontend/src/test/AiPanel.test.tsx
- **O que fazer:** Botões Melhorar, Analisar e Dividir no detalhe da tarefa; expor a sugestão de melhoria com botão "Aplicar à tarefa" que grava via API de tarefas; mostrar prioridade, complexidade, horas sugeridas e justificativa; exibir a lista de subtarefas propostas com seleção e exclusão de itens, e botão "Adicionar como tarefas" que chama decompose/apply; spinner e desabilitação durante cada chamada; tratamento de erro de LLM com mensagem amigável.
- **Pronto quando:** `npm run lint`, `npm run test` e `npm run build` passam; o fluxo de aplicar melhoria e o de adicionar subtarefas estão cobertos por teste com a camada api mockada.
- **Testes:** AiPanel.test.tsx (chama a API certa, exibe sugestão, aplica melhoria, seleciona e envia subtarefas, mostra erro de LLM).
- **Gate:** cd frontend && npm run lint && npm run test && npm run build
- **Commit (rascunho):** `add: Painel de IA para melhorar, analisar e dividir tarefas`