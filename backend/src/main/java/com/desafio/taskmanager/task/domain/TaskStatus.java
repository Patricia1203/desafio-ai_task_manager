package com.desafio.taskmanager.task.domain;

/**
 * Ciclo de vida de uma tarefa.
 *
 * <p>As transicoes validas ficam em {@link Task#changeStatus(TaskStatus)};
 * aqui so mora o conjunto de estados possiveis.
 *
 * <p>Os valores sao gravados no banco por {@code @Enumerated(EnumType.STRING)}
 * e viaem no JSON como string. Sao os mesmos em codigo, banco e contrato: um
 * valor so, sem camada de traducao entre eles.
 */
public enum TaskStatus {

    TODO,
    IN_PROGRESS,
    DONE;

    /** Estado inicial de toda tarefa recem-criada. */
    public static final TaskStatus INITIAL = TODO;
}
