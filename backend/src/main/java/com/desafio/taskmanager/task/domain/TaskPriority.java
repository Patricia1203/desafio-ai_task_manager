package com.desafio.taskmanager.task.domain;

/** Prioridade da tarefa. A analise de IA (RF-11) devolve um valor desta enum. */
public enum TaskPriority {

    LOW,
    MEDIUM,
    HIGH;

    /** Prioridade usada quando o chamador nao informa. */
    public static final TaskPriority DEFAULT = MEDIUM;
}