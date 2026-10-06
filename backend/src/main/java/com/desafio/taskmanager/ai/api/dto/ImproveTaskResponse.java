package com.desafio.taskmanager.ai.api.dto;

/**
 * Sugestao de melhoria devolvida pelo POST /improve (US-020): titulo e
 * descricao em portugues, sem persistir nada — aplicar ou nao e decisao de
 * quem chama (RF-10, RF-24).
 */
public record ImproveTaskResponse(String titulo, String descricao) {
}
