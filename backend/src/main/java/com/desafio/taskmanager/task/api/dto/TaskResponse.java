package com.desafio.taskmanager.task.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

/**
 * Tarefa exposta na API. Record imutavel: a entidade JPA nunca sai do backend
 * (ver CONVENTIONS.md). O pai aparece so como {@code idTarefaPai}.
 *
 * <p><b>Por que o nome do record e o nome do campo.</b> O Jackson serializa o
 * record pelo nome do componente, entao {@code titulo} aqui e {@code "titulo"}
 * no JSON. Nao ha {@code @JsonProperty} nem camada de traducao: o contrato e o
 * codigo. Decisao do usuario, contrato em portugues.
 */
public record TaskResponse(
        UUID id,
        String titulo,
        String descricao,
        TaskStatus status,
        TaskPriority prioridade,
        LocalDate prazo,
        UUID idTarefaPai,
        Instant criadoEm,
        Instant atualizadoEm) {
}
