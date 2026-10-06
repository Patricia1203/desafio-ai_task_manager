package com.desafio.taskmanager.assistant.infra;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.domain.ChatMessage;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a tabela {@code chat_messages} (F04). A janela de historico le por
 * conversa em ordem cronologica; o {@code id} e o desempate para que o relogio
 * em microssegundos do Postgres nao inverta mensagens gravadas no mesmo instante.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);
}