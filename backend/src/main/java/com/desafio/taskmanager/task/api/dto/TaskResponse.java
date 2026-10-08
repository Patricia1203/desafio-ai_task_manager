package com.desafio.taskmanager.task.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;
import com.desafio.taskmanager.task.domain.TimeUnit;

/**
 * Tarefa exposta na API. Record imutavel: a entidade JPA nunca sai do backend
 * (ver CONVENTIONS.md). O pai aparece so como {@code parentId}.
 *
 * <p><b>Por que o nome do record e o nome do campo.</b> O Jackson serializa o
 * record pelo nome do componente, entao {@code title} aqui e {@code "title"}
 * no JSON. Nao ha {@code @JsonProperty} nem camada de traducao: o contrato e o
 * codigo. Decisao da revisao F06: contrato em ingles (documentacao do desafio
 * exige ingles), rotulos em portugues ficam so na UI.
 *
 * <p>{@code subtaskCount} (T-F07-02) e o numero de subtarefas quando a tarefa e
 * raiz; vem preenchido so na listagem publica (contagem agrupada no banco, sem
 * N+1), e 0 nos demais usos.
 *
 * <p>{@code estimatedTime}/{@code estimatedUnit} (F12) e o tempo estimado para
 * realizar a tarefa; ambos nulos quando nao ha estimativa. Valor e unidade
 * andam juntos garantido pela entidade.
 *
 * <p>{@code areaId} (F14) e a area de trabalho da tarefa; nulo quando a tarefa
 * nao tem area. O titulo da area e resolvido no frontend (mapa de areas).
 */
public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Double estimatedTime,
        TimeUnit estimatedUnit,
        UUID parentId,
        UUID areaId,
        Instant createdAt,
        Instant updatedAt,
        long subtaskCount) {
}