package com.desafio.taskmanager.assistant.application.tools.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

/**
 * Resultado enxuto de uma ferramenta somente-leitura do assistente (F04).
 *
 * <p>Mesmos nomes de campos do {@code TaskResponse} (contrato em portugues),
 * sem os campos que o modelo nao precisa para responder: o que vai ao contexto
 * e a identidade, o estado e o prazo. A conversao e campo a campo para a
 * entidade nao vazar do nucleo.
 */
public record TaskToolResult(
        UUID id,
        String titulo,
        TaskStatus status,
        TaskPriority prioridade,
        LocalDate prazo) {

    public static TaskToolResult de(Task task) {
        return new TaskToolResult(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate());
    }
}