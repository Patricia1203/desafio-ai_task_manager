package com.desafio.taskmanager.assistant.infra;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.domain.ChatConversation;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a tabela {@code chat_conversations} (F04), com a lista do historico
 * (F10): as conversas saem da mais recente para a mais antiga pela ultima
 * atividade ({@code updated_at}), a mesma ordem que o ChatGPT mostra.
 */
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    List<ChatConversation> findAllByOrderByUpdatedAtDesc();
}