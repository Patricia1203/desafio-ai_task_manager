package com.desafio.taskmanager.task.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TimeUnit;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

        LocalDate dueDate,

        /** F12: tempo estimado para realizar; valor sem unidade assume HOURS. */
        @DecimalMin(value = "0", inclusive = false, message = "estimatedTime deve ser maior que zero")
        @DecimalMax(value = "200", message = "estimatedTime deve ter no maximo 200")
        Double estimatedTime,

        /**
         * F12: unidade do tempo estimado (HOURS ou DAYS). Opcional.
         */
        TimeUnit estimatedUnit,

        /**
         * F14: id da area de trabalho. PUT substitui o conteudo: {@code null}
         * remove a area atual.
         */
        UUID areaId) {
}
