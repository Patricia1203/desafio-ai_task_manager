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
 *
 * <p>Desde a F10 a conversa carrega o {@code title} do historico (a primeira
 * mensagem do usuario, cortada em 200 caracteres por {@link #defineTituloPadrao})
 * e a {@code updatedAt} de ultima atividade, virada por {@link #toca()} a cada
 * turno para a lista de conversas ordenar do ChatGPT para baixo.
 */
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    /** Titulo maximo do historico; alinhado ao {@code max-title-length} das tasks. */
    private static final int MAX_TITULO = 200;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "title", length = MAX_TITULO)
    private String title;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Construtor para o JPA. Nao use: toda conversa nasce por {@link #nova()}. */
    protected ChatConversation() {
    }

    private ChatConversation(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
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

    public String getTitle() {
        return title;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Titulo do historico a partir da primeira mensagem do usuario (F10): so
     * preenche quando ainda nao ha titulo — conversa retomada nao sobrescreve.
     * Espacos e quebras viram um espaco unico; acima de {@value #MAX_TITULO}
     * caracteres o texto e cortado com reticencias.
     */
    public void defineTituloPadrao(String primeiraMensagem) {
        if (title != null || primeiraMensagem == null || primeiraMensagem.isBlank()) {
            return;
        }
        String limpo = primeiraMensagem.replaceAll("\\s+", " ").trim();
        title = limpo.length() > MAX_TITULO
                ? limpo.substring(0, MAX_TITULO - 1).trim() + "\u2026"
                : limpo;
    }

    /** Registra a ultima atividade (F10): chamado a cada turno antes da gravacao. */
    public void toca() {
        updatedAt = Instant.now();
    }
}