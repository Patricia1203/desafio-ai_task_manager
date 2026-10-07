package com.desafio.taskmanager.ai.api.dto;

import java.util.List;

/**
 * Sugestoes do POST /api/ai/tasks/{id}/decompose (US-022): so propoe; criar e
 * papel do POST /decompose/apply, que recebe a lista aceita pelo usuario
 * (RF-12, RF-14).
 *
 * <p>Campos em ingles (RNF-10); o record {@code TaskDecomposition} da porta
 * continua em ingles.
 */
public record DecomposeTaskResponse(List<SubtaskSuggestion> subtasks) {

    /**
     * Uma sugestao de subtarefa. {@code estimatedHours} e opcional porque o
     * schema do modelo permite omitir (design.md).
     */
    public record SubtaskSuggestion(String title, String description, Double estimatedHours) {
    }
}
