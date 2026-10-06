package com.desafio.taskmanager.ai.api.dto;

import java.util.List;

/**
 * Sugestoes do POST /api/ai/tasks/{id}/decompose (US-022): so propoe; criar e
 * papel do POST /decompose/apply, que recebe a lista aceita pelo usuario
 * (RF-12, RF-14).
 *
 * <p>Campos em portugues (RF-24); o record {@code TaskDecomposition} da porta
 * continua em ingles.
 */
public record DecomposeTaskResponse(List<SubtarefaSugerida> subtarefas) {

    /**
     * Uma sugestao de subtarefa. {@code horasEstimadas} e opcional porque o
     * schema do modelo permite omitir (design.md).
     */
    public record SubtarefaSugerida(String titulo, String descricao, Double horasEstimadas) {
    }
}
