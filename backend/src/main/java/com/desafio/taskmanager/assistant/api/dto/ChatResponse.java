package com.desafio.taskmanager.assistant.api.dto;

import java.util.UUID;

/**
 * Resposta do POST /assistant/chat (US-030): a conversa em que o turno foi
 * gravado e a resposta do assistente (RNF-10).
 */
public record ChatResponse(UUID conversationId, String response) {
}