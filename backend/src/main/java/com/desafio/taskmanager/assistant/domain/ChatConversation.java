package com.desafio.taskmanager.assistant.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Conversa do assistente (F04). Entidade JPA, um registro por conversa; as
 * mensagens ficam em {@code chat_messages}, referenciadas por conversationId.
 *
 * <p>O id e o createdAt sao gerados aqui (mesma regra da Task): a entidade ja
 * nasce consistente antes de chegar ao banco.
 */
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Construtor para o JPA. Nao use: toda conversa nasce por {@link #nova()}. */
    protected ChatConversation() {
    }

    private ChatConversation(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public static ChatConversation nova() {
        return new ChatConversation(UUID.randomUUID(), Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}