package com.desafio.taskmanager.ai.port.dto;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Análise de uma tarefa (RF-11): prioridade sugerida, complexidade, horas
 * estimadas e a justificativa. Interpretar e aplicar é da camada de api e
 * serviço — a análise nunca altera a tarefa sozinha.
 *
 * <p>{@code estimatedHours} é {@link Double} e não {@code double} de propósito:
 * campo ausente no JSON vira null e o validador rejeita com mensagem própria,
 * em vez de virar 0,0 e passar como horas de verdade.
 */
public record TaskAnalysis(
        @JsonProperty("priority") TaskPriority priority,
        @JsonProperty("complexity") TaskComplexity complexity,
        @JsonProperty("estimatedHours") Double estimatedHours,
        @JsonProperty("reason") String reason) {
}
