# tasks.md — F04-ai-assistant

### T-F04-01 — Entidades, migração e repositório Chat
- Status: pending
- Reqs: RF-18
- Depende de: T-F01-04
- Arquivos: assistant/domain/*, assistant/infra/*, resources/db/migration/V3__create_chat.sql
- O que fazer: ChatConversation, ChatMessage com relacionamento.
- Pronto quando: Migração válida.
- Testes: TST-04.
- Gate: cd backend && mvn -q test -Dtest=*Chat*Repository*Test* 2>&1 | tail -1
- Commit: dd: Entidades e migração de chat (histórico persistido)

### T-F04-02 — Ferramentas somente-leitura (Tools)
- Status: pending
- Reqs: RF-19
- Depende de: T-F04-01, T-F02-03
- Arquivos: assistant/application/tools/*
- O que fazer: getPendingTasks, getOverdueTasks, getTaskById, getTasksByPriority, getTasksDueSoon(days), getTaskSummary. Retorno enxuto.
- Pronto quando: Métodos retornam apenas leitura, DTOs enxutos.
- Testes: TST-01 unitários.
- Gate: cd backend && mvn -q test -Dtest=*AssistantTools*Test* 2>&1 | tail -2
- Commit: dd: Ferramentas somente-leitura para assistente

### T-F04-03 — Serviço de chat com grounding + memória
- Status: pending
- Reqs: RF-15..RF-17,RF-24,RNF-13,RNF-16
- Depende de: T-F04-02,T-F03-02
- Arquivos: assistant/application/AssistantService.java, assistant/api/AssistantController.java, resources/prompts/assistant-system.st
- O que fazer: Grounding estrito (só contexto), data atual injetada, histórico por conversationId, persistido. Se não suporta tools, registrar decisão.
- Pronto quando: Responde só com base em contexto; mantém histórico.
- Testes: TST-01,TST-02,TST-03.
- Gate: cd backend && mvn -q test -Dtest=*Assistant*Test* 2>&1 | tail -8
- Commit: dd: Serviço e endpoints do assistente com grounding

### T-F04-04 — Frontend tela do assistente
- Status: pending
- Reqs: RF-22
- Depende de: T-F04-03
- Arquivos: frontend/src/api/assistant.ts, frontend/src/pages/Assistant.tsx, frontend/src/components/assistant/Chat.tsx
- O que fazer: Chat com histórico, indicador digitação, nova conversa, conversationId mantido.
- Pronto quando: Build passa, testes com mocks.
- Testes: Vitest + Testing Library.
- Gate: cd frontend && npm run build && npx vitest run --reporter=basic 2>&1 | tail -5
- Commit: dd: Tela do assistente (chat com histórico)
