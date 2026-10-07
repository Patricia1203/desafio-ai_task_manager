package com.desafio.taskmanager.ai.api.dto;

/**
 * Sugestao de melhoria devolvida pelo POST /improve (US-020): titulo e
 * descricao em ingles, sem persistir nada — aplicar ou nao e decisao de
 * quem chama (RF-10, RNF-10).
 */
public record ImproveTaskResponse(String title, String description) {
}
