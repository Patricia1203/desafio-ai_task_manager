package com.desafio.taskmanager.task.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

/**
 * Tarefa exposta na API. Record imutavel: a entidade JPA nunca sai do backend
 * (ver CONVENTIONS.md). O pai aparece so como {@code parentId}.
 *
 * <p><b>Por que o nome do record e o nome do campo.</b> O Jackson serializa o
 * record pelo nome do componente, entao {@code title} aqui e {@code "title"}
 * no JSON. Nao ha {@code @JsonProperty} nem camada de traducao: o contrato e o
 * codigo. Decisao da revisao F06: contrato em ingles (documentacao do desafio
 * exige ingles), rotulos em portugues ficam so na UI.
 */
public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        UUID parentId,
        Instant createdAt,
        Instant updatedAt) {
}
