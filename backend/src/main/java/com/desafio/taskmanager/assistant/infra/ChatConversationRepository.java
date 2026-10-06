package com.desafio.taskmanager.assistant.infra;

import java.util.UUID;

import com.desafio.taskmanager.assistant.domain.ChatConversation;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a tabela {@code chat_conversations} (F04).
 */
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
}