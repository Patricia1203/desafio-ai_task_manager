package com.desafio.taskmanager.ai.api.dto;

import com.desafio.taskmanager.ai.port.dto.TaskComplexity;
import com.desafio.taskmanager.task.domain.TaskPriority;

/**
 * Analise devolvida pelo POST /api/ai/tasks/{id}/analyze (US-021): os campos
 * sao os do design.md, em ingles (RNF-10). A analise nunca altera a tarefa:
 * interpretar e aplicar e decisao de quem chama (RF-11).
 *
 * <p>Os <b>valores</b> dos enums sao os que o schema do provedor pede:
 * prioridade e a propria do dominio (LOW/MEDIUM/HIGH) e complexidade e
 * LOW/MEDIUM/HIGH, conforme o design.md.
 */
public record AnalyzeTaskResponse(
        TaskPriority priority,
        TaskComplexity complexity,
        Double estimatedHours,
        String reason) {
}
