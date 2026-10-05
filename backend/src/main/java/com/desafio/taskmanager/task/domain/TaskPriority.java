package com.desafio.taskmanager.task.domain;

/**
 * Prioridade da tarefa. A analise de IA (RF-11) devolve um valor desta enum.
 *
 * <p>Como em {@link TaskStatus}, o valor e o mesmo em codigo, banco e JSON:
 * nao existe traducao entre as tres camadas.
 */
public enum TaskPriority {

    BAIXA,
    MEDIA,
    ALTA;

    /** Prioridade usada quando o chamador nao informa. */
    public static final TaskPriority DEFAULT = MEDIA;
}
