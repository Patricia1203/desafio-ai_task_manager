-- V5__chats_titulo_e_atividade.sql
-- Revisao F10: o historico de conversas precisa de titulo (primeira mensagem
-- do usuario) e da data de ultima atividade para a lista ordenar do ChatGPT
-- para baixo (conversa mais recente primeiro).

ALTER TABLE chat_conversations ADD COLUMN title VARCHAR(200);
ALTER TABLE chat_conversations ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Backfill do titulo: a primeira mensagem USER de cada conversa. Conversa sem
-- mensagem ganha um titulo neutro (na pratica nao existe: turno so e gravado
-- apos a resposta da IA).
UPDATE chat_conversations c
SET title = COALESCE(
    (SELECT LEFT(btrim(m.content), 200)
       FROM chat_messages m
      WHERE m.conversation_id = c.id AND m.role = 'USER'
      ORDER BY m.created_at ASC, m.id ASC
      LIMIT 1),
    'Conversa sem titulo');

-- Backfill da atividade: a ultima mensagem de cada conversa.
UPDATE chat_conversations c
SET updated_at = m.ultima
FROM (SELECT conversation_id, MAX(created_at) AS ultima FROM chat_messages GROUP BY conversation_id) m
WHERE m.conversation_id = c.id;

CREATE INDEX idx_chat_conversations_updated_at ON chat_conversations (updated_at DESC);

COMMENT ON COLUMN chat_conversations.title      IS 'Titulo do historico: primeira mensagem do usuario, 200 caracteres';
COMMENT ON COLUMN chat_conversations.updated_at IS 'Ultima atividade (atualizada a cada turno) para ordenar o historico';