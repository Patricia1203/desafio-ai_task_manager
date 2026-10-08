package com.desafio.taskmanager.assistant.api.dto;

import java.time.Instant;

/**
 * Mensagem exposta pelo GET /assistant/conversations/{id}/messages
 * (T-F06-10): o {@code role} vem em minusculo (user/assistant) e o
 * {@code createdAt} e o instante gravado, em ISO-8601 — a UI usa esses
 * campos para restaurar a conversa em andamento depois de um reload.
 */
public record ChatMessageResponse(Long id, String role, String content, Instant createdAt) {
}