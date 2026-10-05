package com.desafio.taskmanager.task.domain;

/**
 * Ciclo de vida de uma tarefa.
 *
 * <p>As transicoes validas ficam em {@link Task#changeStatus(TaskStatus)};
 * aqui so mora o conjunto de estados possiveis.
 */
public enum TaskStatus {

    TODO,
    IN_PROGRESS,
    DONE;

    /** Estado inicial de toda tarefa recem-criada. */
    public static final TaskStatus INITIAL = TODO;
}