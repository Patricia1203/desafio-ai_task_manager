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
 * Corpo do POST /api/tasks.
 *
 * <p>As mensagens das constraints vao para o ProblemDetail (propriedade
 * {@code errors}) e sao lidas pelo usuario, entao vao em portugues. Os nomes
 * dos componentes sao os campos do JSON (contrato em ingles desde a revisao
 * F06).
 */
public record CreateTaskRequest(

        @NotBlank(message = "titulo e obrigatorio")
        @Size(max = 200, message = "titulo deve ter no maximo 200 caracteres")
        String title,

        @Size(max = 5000, message = "descricao deve ter no maximo 5000 caracteres")
        String description,

        /** Opcional: sem valor a tarefa nasce MEDIUM. */
        TaskPriority priority,

        /** Prazo em data (sem hora). Opcional. */
        LocalDate dueDate,

        /**
         * F12: tempo estimado para realizar, na unidade de {@code estimatedUnit}.
         * Opcional; valor sem unidade assume HOURS no dominio.
         */
        @DecimalMin(value = "0", inclusive = false, message = "estimatedTime deve ser maior que zero")
        @DecimalMax(value = "200", message = "estimatedTime deve ter no maximo 200")
        Double estimatedTime,

        /**
         * F12: unidade do tempo estimado (HOURS ou DAYS). Opcional.
         */
        TimeUnit estimatedUnit,

        /** F14: id da area de trabalho da tarefa. Opcional. */
        UUID areaId) {
}
