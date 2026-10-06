-- V3__create_chat.sql
-- Tabelas do historico de conversa do assistente. Donas da F04 (ai-assistant).
-- Colunas em snake_case; as entidades ChatConversation e ChatMessage mapeiam
-- explicitamente cada uma.
--
-- role e o enum ChatRole em ingles, contrato interno do banco (mesmo criterio
-- dos valores de TaskComplexity da F03): alterar um valor aqui exige alterar a
-- enum e vice-versa.

CREATE TABLE chat_conversations (
    id          UUID        PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id               BIGSERIAL      PRIMARY KEY,
    conversation_id  UUID           NOT NULL REFERENCES chat_conversations (id) ON DELETE CASCADE,
    role             VARCHAR(20)    NOT NULL,
    content          VARCHAR(5000)  NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_chat_messages_role    CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_chat_messages_content CHECK (length(btrim(content)) > 0)
);

-- indice composto: a janela de historico (T-F04-03) sempre filtra por
-- conversation_id e ordena por created_at
CREATE INDEX idx_chat_messages_conversation_created ON chat_messages (conversation_id, created_at);

COMMENT ON TABLE  chat_messages                 IS 'Mensagens do historico de conversa do assistente';
COMMENT ON COLUMN chat_messages.conversation_id IS 'Conversa a que a mensagem pertence (ON DELETE CASCADE)';
COMMENT ON COLUMN chat_messages.content         IS 'Texto da mensagem, limitado a 5000 caracteres';