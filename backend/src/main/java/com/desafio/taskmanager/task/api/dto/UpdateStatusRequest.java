package com.desafio.taskmanager.task.api.dto;

import com.desafio.taskmanager.task.domain.TaskStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do PATCH /api/tasks/{id}/status.
 *
 * <p>Um valor fora da enum chega como JSON invalido e cai em
 * {@code HttpMessageNotReadableException}, que o handler global traduz em 400.
 *
 * <p>{@code completeSubtasks} e opcional (F09): so tem efeito quando
 * {@code status == DONE} — conclui tambem as subtarefas pendentes do pai na mesma
 * transacao. Ausente/{@code false} mantem o comportamento original (so o pai).
 */
public record UpdateStatusRequest(

        @NotNull(message = "status e obrigatorio")
        TaskStatus status,

        Boolean completeSubtasks) {
}