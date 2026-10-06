# tasks.md — F04-ai-assistant

### T-F04-01 — Entidades e migração do histórico de conversa
- **Status:** done
- **Reqs:** RF-17, RF-18, RNF-21, TST-04
- **Depende de:** T-F01-04
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/assistant/domain/ChatConversation.java, ChatMessage.java, ChatRole.java, backend/src/main/java/com/desafio/taskmanager/assistant/infra/ChatConversationRepository.java, ChatMessageRepository.java, backend/src/main/resources/db/migration/V3__create_chat.sql, backend/src/test/java/.../assistant/infra/ChatRepositoryTest.java
- **O que fazer:** Persistir conversas e mensagens em PostgreSQL. Tabela de conversa com id UUID e createdAt; tabela de mensagem com id, conversationId, role, conteúdo com tamanho limitado e createdAt. Índices em conversationId e createdAt para a janela de histórico.
- **Pronto quando:** migração aplicada em Postgres real; é possível gravar, ler e listar as mensagens de uma conversa em ordem cronológica.
- **Testes:** ChatRepositoryTest (grava e lê conversa com mensagens, ordenação por createdAt, isolamento entre conversas) com Testcontainers.
- **Gate:** mvn -q test -Dtest=ChatRepositoryTest
- **Commit (rascunho):** `add: Entidades e migração do histórico de conversa`

### T-F04-02 — Ferramentas somente-leitura do assistente
- **Status:** done (commit `0eaed8d`, 2026-10-06)
- **Reqs:** RF-16, RF-19, RNF-13, TST-01
- **Depende de:** T-F04-01, T-F02-03
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/assistant/application/tools/TaskQueryTools.java, backend/src/main/java/com/desafio/taskmanager/assistant/application/tools/dto/TaskToolResult.java, backend/src/main/java/com/desafio/taskmanager/common/config/AssistantLimitsProperties.java, backend/src/test/java/.../assistant/application/tools/TaskQueryToolsTest.java
- **O que fazer:** Implementar getPendingTasks, getOverdueTasks, getTaskById, getTasksByPriority, getTasksDueSoon(days) e getTaskSummary. Somente leitura, sem método de escrita; resultado em DTO enxuto e limitado; validação do parâmetro days.
- **Pronto quando:** cada ferramenta devolve o resultado enxuto respeitando o limite configurado; não existe nenhum caminho de escrita; getTaskById devolve vazio para id inexistente em vez de erro.
- **Testes:** TaskQueryToolsTest com repositório mockado: filtros corretos, limites aplicados, id inexistente, days inválido e garantia de que nenhuma dependência de escrita é exposta.
- **Gate:** mvn -q test -Dtest=TaskQueryToolsTest
- **Commit (rascunho):** `add: Ferramentas somente-leitura para o assistente`
- **Evidências:** TaskQueryTools com 6 ferramentas limitadas por `app.assistant.max-tool-results`; getTasksDueSoon valida 1..365; getTaskById usa Optional (vazio p/ id inexistente). Extras além da lista prevista: `findByStatusInOrderByCreatedAtDesc` no TaskRepository (query nova p/ pendentes) + caso em TaskRepositoryTest; `app.assistant.max-tool-results` em application.yml; AssistantLimitsProperties habilitado via `@EnableConfigurationProperties` do AiAdapterConfig (record @Component quebra o binding — decisão registrada). Gate `mvn -q test -Dtest=TaskQueryToolsTest,TaskRepositoryTest`: 24 verdes; suíte completa 203/0/0/0 (17 suítes).

### T-F04-03 — Serviço de chat com grounding e memória
- **Status:** pending
- **Reqs:** RF-15, RF-16, RF-17, RF-18, RF-19, RF-24, RNF-10, RNF-13, RNF-14, RNF-15, ERR-03, ERR-04, ERR-05, TST-03
- **Depende de:** T-F04-02, T-F03-02
- **Arquivos (criar/alterar):** backend/src/main/java/com/desafio/taskmanager/assistant/port/AssistantPort.java, backend/src/main/java/com/desafio/taskmanager/assistant/application/AssistantService.java, backend/src/main/java/com/desafio/taskmanager/assistant/api/AssistantController.java, backend/src/main/java/com/desafio/taskmanager/assistant/api/dto/ChatRequest.java, ChatResponse.java, backend/src/main/java/com/desafio/taskmanager/ai/adapter/SpringAiAssistantAdapter.java, backend/src/main/resources/prompts/assistant-system.st, backend/src/test/java/.../assistant/application/AssistantServiceTest.java, backend/src/test/java/.../assistant/api/AssistantControllerTest.java
- **O que fazer:** System prompt com grounding estrito (responder apenas com base nos dados das ferramentas, dizer que não encontrou quando não houver dado, recusar assuntos fora de tarefas), data atual injetada pelo backend, delimitação explícita do conteúdo do usuário como dado não confiável, e nenhuma ferramenta de escrita. Histórico por conversationId, com janela limitada e persistido no banco. Tool calling registrado apenas se o modelo suportar; caso contrário, fallback de contexto injetado com a decisão registrada.
- **Pronto quando:** a resposta se baseia só nos dados disponíveis; o histórico é retomado pelo conversationId; mensagem acima do limite é rejeitada com 400; falha do LLM vira 502/503 conforme ERR-03/ERR-05.
- **Testes:** AssistantServiceTest com a porta fake (grounding preservado, histórico retomado, janela limitada, conversa nova quando não há conversationId); AssistantControllerTest (200/400/502/503 e formato ProblemDetail).
- **Gate:** mvn -q test -Dtest=AssistantServiceTest,AssistantControllerTest
- **Commit (rascunho):** `add: Serviço e endpoints do assistente com grounding e memória`

### T-F04-04 — Tela do assistente
- **Status:** pending
- **Reqs:** RF-22, RNF-03, RF-15
- **Depende de:** T-F04-03
- **Arquivos (criar/alterar):** frontend/src/api/assistant.ts, frontend/src/types/assistant.ts, frontend/src/pages/AssistantPage.tsx, frontend/src/components/assistant/ChatWindow.tsx, MessageBubble.tsx, frontend/src/router.tsx, frontend/src/components/layout/AppLayout.tsx, frontend/src/test/AssistantPage.test.tsx
- **O que fazer:** Tela de conversa com lista de mensagens, indicador de digitação enquanto aguarda o backend, campo de entrada com envio por Enter, maintenance do conversationId na sessão e botão "nova conversa" que limpa o histórico local. Estados de loading, erro e vazio.
- **Pronto quando:** `npm run lint`, `npm run test` e `npm run build` passam; o envio mantém o conversationId devolvido e nova conversa reinicia o estado.
- **Testes:** AssistantPage.test.tsx (renderiza mensagens, envia mensagem e exibe resposta, mantém conversationId, nova conversa, erro do LLM) com a camada api mockada.
- **Gate:** cd frontend && npm run lint && npm run test && npm run build
- **Commit (rascunho):** `add: Tela do assistente com histórico de conversa`