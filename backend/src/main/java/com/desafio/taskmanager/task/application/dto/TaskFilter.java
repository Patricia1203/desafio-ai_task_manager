package com.desafio.taskmanager.task.application.dto;

import java.util.UUID;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

/**
 * Criterio de listagem de tarefas (RF-02). Os campos sao opcionais e compoem
 * entre si: os ausentes simplesmente nao entram na Specification.
 *
 * <p>Sem pageable de proposito — a paginacao e argumento separado de
 * {@code TaskService#list}, para o mesmo filtro servir tanto para a listagem
 * paginada da tela quanto para uma leitura completa do assistente (F04).
 */
public record TaskFilter(TaskStatus status, TaskPriority priority, UUID areaId, String title) {

    /** Variante sem area, usada pela maior parte dos fluxos atuais. */
    public TaskFilter(TaskStatus status, TaskPriority priority) {
        this(status, priority, null, null);
    }

    /** Variante com area e sem titulo (F14). */
    public TaskFilter(TaskStatus status, TaskPriority priority, UUID areaId) {
        this(status, priority, areaId, null);
    }

    /** Sem filtro: devolve a listagem inteira. */
    public static TaskFilter all() {
        return new TaskFilter(null, null);
    }

    public static TaskFilter byStatus(TaskStatus status) {
        return new TaskFilter(status, null);
    }

    public static TaskFilter byPriority(TaskPriority priority) {
        return new TaskFilter(null, priority);
    }

    public boolean hasStatus() {
        return status != null;
    }

    public boolean hasPriority() {
        return priority != null;
    }

    /** F14: filtro por area de trabalho ({@code GET /tasks?areaId=}). */
    public boolean hasArea() {
        return areaId != null;
    }

    /** Busca por parte do titulo, ignorando caixa ({@code GET /tasks?title=}). */
    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    /** Verdadeiro quando nenhum criterio foi informado. */
    public boolean isEmpty() {
        return !hasStatus() && !hasPriority() && !hasArea() && !hasTitle();
    }
}