package com.desafio.taskmanager.task.api.dto;

import java.time.LocalDate;

import com.desafio.taskmanager.task.domain.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PUT /api/tasks/{id}.
 *
 * <p>PUT e substituicao total: o titulo e obrigatorio. O status nao vem aqui —
 * ele tem endpoint proprio (PATCH /status), que valida as transicoes.
 */
public record UpdateTaskRequest(

        @NotBlank(message = "titulo e obrigatorio")
        @Size(max = 200, message = "titulo deve ter no maximo 200 caracteres")
        String title,

        @Size(max = 5000, message = "descricao deve ter no maximo 5000 caracteres")
        String description,

        TaskPriority priority,

        LocalDate dueDate) {
}
