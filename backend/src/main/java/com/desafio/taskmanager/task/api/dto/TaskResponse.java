package com.desafio.taskmanager.task.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

/**
 * Tarefa exposta na API. Record imutavel: a entidade JPA nunca sai do backend
 * (ver CONVENTIONS.md). O pai aparece so como {@code parentId}.
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