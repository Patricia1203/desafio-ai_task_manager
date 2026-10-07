package com.desafio.taskmanager.assistant.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /assistant/chat (US-030).
 *
 * <p>{@code conversationId} e opcional: ausente cria uma conversa nova; o
 * preenchido retoma o historico persistido. {@code mensagem} replica os limites
 * da entidade {@code ChatMessage} (nao vazia, no maximo 5000 caracteres) para
 * o retorno ser 400 com o campo, nao 422 de regra de negocio.
 */
public record ChatRequest(
        UUID conversationId,
        @NotBlank(message = "mensagem nao pode ser vazia")
        @Size(max = 5000, message = "mensagem deve ter no maximo 5000 caracteres")
        String mensagem) {
}