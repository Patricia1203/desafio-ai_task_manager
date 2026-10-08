# design.md — F10-chat-history

## Decisões
- **Título gravado na conversa, não derivado na leitura.** `chat_conversations.title`
  (VARCHAR 200) é preenchido com a primeira mensagem **USER** da conversa, no `ChatTurnWriter`
  (mesma transação do turno). Alternativa avaliada (derivar por agregado na listagem) foi
  descartada: o backfill existia de qualquer forma e guardar torna a listagem barata.
- **`updatedAt` para ordenar.** Ordenar por `created_at` da conversa deixaria conversas
  retomadas presas na posição original; `chat_conversations.updated_at` é virado a cada
  turno (`ChatConversation.toca()`), então a conversa ativa sobe no histórico como no
  ChatGPT.
- **Sem paginação e sem DELETE/rename.** Aplicação local, um usuário; manter escopo do
  pedido. Renomear/excluir conversas são evoluções futuras anotadas no final.
- **`role` minúsculo no DTO de mensagem.** O contrato da UI usa `user`/`assistant`; o enum
  `ChatRole` serializa `USER`/`ASSISTANT`. O DTO `ConversationMessage` converte para
  minúsculo (contrato EN da RF-24, sem vazar o enum de domínio).
- **Altura consistente da janela.** Bolhas têm `max-height: min(60vh, 640px)`; sem mensagens
  a janela encolhia. `.assistant__janela` ganha `min-height` idêntico e `display:flex`
  coluna com `margin-top:auto` no formulário → vazia ou cheia, tamanho e âncora iguais.

## Fluxo (frontend)
```
monta página ─→ GET /assistant/conversations ─→ sidebar (título + dd/mm/aaaa)
   │                                             (erro de listagem é silencioso)
   ▼
clica conversa ─→ GET /assistant/conversations/{id}
   │ 200 ─→ mensagens na janela, conversationId=id, aria-current, item sobe
   └ 404/erro ─→ messageOf → role=alert (janela atual fica intacta)
enviar ─→ POST /assistant/chat (com conversationId se restaurada)
   └ sucesso ─→ recarrega a lista (título/atividade da conversa atualizada)
```

## Backend
- **Migration `V5__chats_titulo_e_atividade.sql`:** `+title VARCHAR(200)`, `+updated_at
  TIMESTAMPTZ NOT NULL DEFAULT now()`; backfill do título (primeira USER, `LEFT(btrim,200)`)
  e da atividade (`MAX(created_at)` das mensagens); índice `updated_at DESC`.
- **Entidade `ChatConversation`:** campos `title` (nullable — conversa nova define no turno)
  e `updatedAt` (= createdAt ao nascer); `defineTituloPadrao(String)` (só se `title == null`,
  colapsa `\s+` → `" "`, corta em 200 com `…`) e `toca()`.
- **`ChatTurnWriter.gravarTurno`:** `defineTituloPadrao(mensagem)` + `toca()` antes do save
  (conversa nova ganha título; retomada só atualiza atividade).
- **Repos:** `ChatConversationRepository.findAllByOrderByUpdatedAtDesc()`;
  `ChatMessageRepository.historicoCompleto(id)` (asc por `created_at, id`).
- **Service:** `listarConversas()` (fallback `"Conversa"` para título nulo/em branco) e
  `conversa(id)` (404 via `ResourceNotFoundException`), mapeia role para minúsculo.
- **Controller:** `GET /assistant/conversations` e `GET /assistant/conversations/{id}`.
- **Testes:** service (título/collapse/corte 200/retomada não sobrescreve/listagem/fallback/
  mensagens asc/404), controller (200 lista, 200 perfil, 404) e repositório (ordenação por
  atividade, histórico cronológico, título persistido).

## Frontend
- `api/assistant.ts`: `listarConversas()` e `buscarConversa(id)`; `types/assistant.ts`:
  `ConversationSummary`, `ConversationMessage`, `ConversationDetail` (`extends Summary`).
- `AssistantPage`: estado `conversas`/`ativaId`/`carregandoConversa`; carrega ao montar e
  após cada turno; `abrirConversa` restaura mensagens, seta `conversationId` e move o item
  ao topo; `novaConversa` mantém o histórico. `ChatWindow` (inalterado) vira região
  acessível `role="region" aria-label="Conversa"`.
- CSS: `.assistant__corpo` flex, `.assistant__historico` coluna fixa 280px (empilha no
  mobile via media query existente), classes `.assistant__conversa*`, `.assistant__janela`
  com min-height.

## Evoluções futuras (não implementadas)
- Renomear o título na sidebar (patch no `chat_conversations.title`).
- Excluir conversa (a FK já usa `ON DELETE CASCADE` em `chat_messages`).
- Paginação/infinite-scroll na listagem; busca no histórico.