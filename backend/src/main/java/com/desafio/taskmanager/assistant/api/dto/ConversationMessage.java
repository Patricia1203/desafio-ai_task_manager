package com.desafio.taskmanager.assistant.api.dto;

/**
 * Mensagem visivel no perfil de uma conversa (F10). O {@code role} vem em
 * minusculo (user/assistant) para casar com o tipo da UI, em vez do nome do
 * enum {@code ChatRole} (USER/ASSISTANT).
 */
public record ConversationMessage(String role, String content) {
}