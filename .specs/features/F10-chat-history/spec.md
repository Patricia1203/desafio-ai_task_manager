# spec.md — F10-chat-history

> Feature criada em 2026-10-08 a pedido do usuário: "Crie um jeito de eu poder recuperar
> outras conversas na aba de assistência, como se fosse um histórico do ChatGPT com título
> da conversa e ao escolher volta nela". Implementação autorizada ("Depois pode executar
> direto") e com o design da página mantido.

## Objetivo
Permitir recuperar conversas anteriores do assistente na aba Assistência: uma lista
(sidebar) com o título de cada conversa e sua última atividade; ao escolher, a conversa é
restaurada na janela e o `conversationId` é reutilizado para continuar o fio.

## Requisitos
- **Listar conversas:** `GET /assistant/conversations` devolve `[{id, title, updatedAt}]`,
  da mais recente atividade para a mais antiga (padrão ChatGPT). Sem paginação (aplicação
  local de um usuário).
- **Título da conversa:** a primeira mensagem do usuário, com espaços/quebras colapsados e
  cortada em 200 caracteres (reticências quando cortada). Conversas já existentes ganham o
  título por backfill na migration V5. Conversa retomada nunca sobrescreve o título.
- **Atividade:** `updatedAt` em `chat_conversations`, atualizada a cada turno (inclusive
  retomadas); a lista ordena por ela.
- **Restaurar conversa:** `GET /assistant/conversations/{id}` devolve
  `{id, title, updatedAt, messages:[{role, content}]}` com as mensagens na ordem
  cronológica; **404** se o id não existe. `role` em minúsculo (`user`/`assistant`),
  casando com o tipo da UI.
- **Continuar:** ao enviar mensagem na conversa restaurada, o frontend reutiliza o
  `conversationId` (mesmo POST `/assistant/chat`, sem mudança de contrato).
- **UI:** sidebar `.assistant__historico` com título + data (dd/mm/aaaa) por item, o item
  aberto marcado (`aria-current`), estado vazio ("Nenhuma conversa salva.") e fallback de
  título ("Conversa"). No mobile a sidebar empilha sobre a janela. A janela tem altura
  mínima única (vazia ou com mensagens) e o formulário fica fixo embaixo.

## Fora de escopo (decisões)
- Renomear conversas, excluir conversas e "Nova conversa" por item na sidebar **não** foram
  pedidos — ficam como possíveis evoluções (anotadas em `design.md`).
- Não há mudança no fluxo de envio nem no contrato do POST `/assistant/chat`.

## Sucesso
1. A aba Assistência mostra o histórico com título das conversas existentes (backfill nos
   dados reais verificado).
2. Clicar em uma conversa restaura as mensagens e permite continuar enviando com o mesmo
   `conversationId`.
3. Uma conversa nova, após a primeira resposta, aparece na lista com título da primeira
   mensagem; a conversa em que se escreve sobe no histórico.
4. Gates: `mvn test` (257), `npx oxlint`, `npx vitest run` (49), `npx vite build` verdes.