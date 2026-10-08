# tasks.md — F10-chat-history

> Feature criada em 2026-10-08 a pedido do usuário: histórico do assistente no padrão
> ChatGPT (lista com título da conversa; escolher volta na conversa). Implementação
> autorizada ("Depois pode executar direto") mantendo o design da página.

### T-F10-01 — Backend: título/atividade da conversa, listagem e perfil
- **Status:** done (commit `b8a4544`, 2026-10-08)
- **Reqs:** RF-17, RF-18
- **Depends on:** —
- **Arquivos (alterar):** `backend/src/main/resources/db/migration/V5__chats_titulo_e_atividade.sql`, `assistant/domain/ChatConversation.java`, `assistant/application/ChatTurnWriter.java`, `assistant/infra/{ChatConversation,ChatMessage}Repository.java`, `assistant/api/dto/{ConversationSummary,ConversationDetail,ConversationMessage}.java`, `assistant/application/AssistantService.java`, `assistant/api/AssistantController.java`, testes (`AssistantServiceTest`, `AssistantControllerTest`, `ChatRepositoryTest`)
- **O que fazer:** `chat_conversations` ganha `title` (VARCHAR 200; primeira mensagem USER do backfill) e `updated_at` (atividade, backfill da última mensagem; índice DESC). `ChatConversation` ganha `title`/`updatedAt`, `defineTituloPadrao` (colapsa espaços, corta em 200 com reticências, não sobrescreve) e `toca()`. `ChatTurnWriter` deriva o título da conversa nova e vira a atividade a cada turno. `GET /assistant/conversations` (resumo por `updated_at` DESC, fallback de título) e `GET /assistant/conversations/{id}` (mensagens asc, role minúsculo; 404 se não existe).
- **Pronto quando:** endpoints respondem (200/404), título deriva da primeira mensagem (novo e backfill), conversa retomada não sobrescreve título, lista ordena por atividade; suíte `mvn test` verde.
- **Gate:** `mvn test` verde (suíte completa).

### T-F10-02 — Frontend: sidebar de histórico com título e retomada
- **Status:** done (commits `8cf08ce`, `09a0db5`, 2026-10-08)
- **Reqs:** RF-17, RF-18
- **Depends on:** T-F10-01
- **Arquivos (alterar):** `frontend/src/types/assistant.ts`, `frontend/src/api/assistant.ts`, `frontend/src/pages/AssistantPage.tsx`, `frontend/src/components/assistant/ChatWindow.tsx`, `frontend/src/index.css`, `frontend/src/test/AssistantPage.test.tsx`
- **O que fazer:** carregar `GET /assistant/conversations` ao montar e após cada turno; sidebar `.assistant__historico` (título + `dd/mm/aaaa`, `aria-current` no item aberto, `"Nenhuma conversa salva."`, fallback `"Conversa"`); ao clicar, `GET /assistant/conversations/{id}` restaura as mensagens, seta `conversationId` e move o item ao topo; erro mantém a janela atual. Janela com `min-height` consistente (vazia/cheia) e formulário fixo embaixo; histórico empilha no mobile. `ChatWindow` vira região acessível.
- **Pronto quando:** histórico real listado com título/data no dev server; escolher uma conversa restaura e continua com o mesmo conversationId; altura igual em conversa nova e com mensagens; testes de componente novos verdes.
- **Gate:** `npx oxlint` limpo; `npx vitest run` verde; `npx tsc -b && npx vite build` OK; verificação E2E headless.

### T-F10-03 — Docs e rastreabilidade
- **Status:** done (2026-10-08)
- **Reqs:** DOC-01
- **Depends on:** T-F10-01, T-F10-02
- **Arquivos (alterar):** `.specs/features/F10-chat-history/{spec,design,tasks}.md`, `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`
- **O que fazer:** criar spec/design/tasks da F10; RF-17/RF-18 na matriz ganham título/atividade e os endpoints de histórico com os SHAs (`b8a4544`, `8cf08ce`); STATE ganha o resumo e a decisão (título = primeira mensagem, ordenação por atividade, sem paginação/rename/delete).
- **Pronto quando:** matriz e STATE refletem o recurso com os commits; sem pendências não justificadas.
- **Gate:** `grep -c "done (parcial"` continua 0; `mvn test` e `npx vitest run` verdes.