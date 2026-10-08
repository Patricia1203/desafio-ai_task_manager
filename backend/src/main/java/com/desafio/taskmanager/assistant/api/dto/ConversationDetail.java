package com.desafio.taskmanager.assistant.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Perfil de uma conversa do historico (F10): os dados do resumo mais as
 * mensagens na ordem cronologica, para a UI restaurar a conversa escolhida.
 */
public record ConversationDetail(
        UUID id,
        String title,
        Instant updatedAt,
        List<ConversationMessage> messages) {
}