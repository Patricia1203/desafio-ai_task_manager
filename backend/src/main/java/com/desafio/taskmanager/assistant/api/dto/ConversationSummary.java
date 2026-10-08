package com.desafio.taskmanager.assistant.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Item do historico de conversas (F10): o que a sidebar do assistente precisa
 * para listar e retomar — id, titulo (primeira mensagem do usuario) e a data
 * de ultima atividade. Campos em ingles, mesmo contrato da RF-24.
 */
public record ConversationSummary(UUID id, String title, Instant updatedAt) {
}