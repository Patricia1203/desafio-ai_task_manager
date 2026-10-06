package com.desafio.taskmanager.assistant.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.desafio.taskmanager.common.error.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mensagem do historico de uma conversa (F04). Entidade JPA.
 *
 * <p>conversationId e uma referencia fraca para a {@link ChatConversation}: o
 * banco garante a integridade via FK com {@code ON DELETE CASCADE} e somente a
 * janela de historico (T-F04-03) precisa da conversa, nunca o inverso.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatRole role;

    @Column(name = "content", nullable = false, length = 5000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Construtor para o JPA. Nao use: mensagem nasce pelo construtor com validacao. */
    protected ChatMessage() {
    }

    public ChatMessage(UUID conversationId, ChatRole role, String content) {
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId nao pode ser nulo");
        this.role = Objects.requireNonNull(role, "role nao pode ser nulo");
        this.content = requireContent(content);
        this.createdAt = Instant.now();
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessRuleException("conteudo de mensagem nao pode ser vazio");
        }
        if (content.length() > 5000) {
            throw new BusinessRuleException("conteudo de mensagem deve ter no maximo 5000 caracteres");
        }
        return content;
    }

    public Long getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public ChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}