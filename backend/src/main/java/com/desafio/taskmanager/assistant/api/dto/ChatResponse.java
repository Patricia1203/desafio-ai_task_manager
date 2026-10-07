package com.desafio.taskmanager.assistant.api.dto;

import java.util.UUID;

/**
 * Resposta do POST /assistant/chat (US-030): a conversa em que o turno foi
 * gravado e a resposta do assistente, em portugues (RF-24).
 */
public record ChatResponse(UUID conversationId, String resposta) {
}