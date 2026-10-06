package com.desafio.taskmanager.assistant.domain;

/**
 * Papel do autor de uma mensagem do historico (F04).
 *
 * <p>Valores em ingles (USER/ASSISTANT) porque sao contrato interno da tabela
 * {@code chat_messages}, nao o JSON publico da API (RF-24) — mesma convencao
 * dos valores de {@code TaskComplexity} na F03.
 */
public enum ChatRole {
    USER,
    ASSISTANT
}