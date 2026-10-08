package com.desafio.taskmanager.ai.api.dto;

import com.desafio.taskmanager.task.domain.TaskPriority;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /api/ai/tasks/{id}/analysis/apply (F13).
 *
 * <p>A IA sempre propoe uma prioridade ({@code priority}) e um total de horas
 * ({@code estimatedHours}, opcional). O servidor recalcula o tempo conforme a
 * regra raiz × subtarefa em vez de gravar o valor cegamente — aqui esta apenas
 * a sugestao bruta da analise.
 */
public record ApplyAnalysisRequest(

        @NotNull(message = "priority e obrigatoria")
        TaskPriority priority,

        @DecimalMin(value = "0", inclusive = false, message = "estimatedHours deve ser maior que zero")
        @DecimalMax(value = "200", message = "estimatedHours deve ter no maximo 200")
        Double estimatedHours) {
}