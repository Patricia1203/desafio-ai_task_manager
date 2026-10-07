package com.desafio.taskmanager.assistant.application.tools.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

/**
 * Resultado enxuto de uma ferramenta somente-leitura do assistente (F04).
 *
 * <p>Os nomes dos campos estao em ingles (RNF-10). O que vai ao contexto do
 * modelo e a identidade, o estado e o prazo. A conversao e campo a campo para a
 * entidade nao vazar do nucleo.
 */
public record TaskToolResult(
        UUID id,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {

    public static TaskToolResult de(Task task) {
        return new TaskToolResult(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate());
    }
}