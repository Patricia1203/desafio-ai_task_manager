package com.desafio.taskmanager.ai.port.dto;

import com.desafio.taskmanager.task.domain.TaskPriority;

/**
 * Contexto mínimo que os prompts recebem (RNF-11): título, descrição e a
 * prioridade atual — o que os três casos de uso usam para decidir. Um campo a
 * mais aqui seria dado trafegando para o modelo sem necessidade.
 *
 * <p>Os três são obrigatórios: título e descrição são a matéria-prima de todo
 * prompt, e toda {@code Task} nasce com prioridade ({@code DEFAULT = MEDIUM});
 * null aqui indicaria erro de mapeamento em quem monta o contexto, não caso
 * de borda.
 */
public record TaskAiContext(
        String title,
        String description,
        TaskPriority priority) {
}
