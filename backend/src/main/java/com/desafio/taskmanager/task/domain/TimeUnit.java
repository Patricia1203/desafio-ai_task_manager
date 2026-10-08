package com.desafio.taskmanager.task.domain;

/**
 * Unidade do tempo estimado para realizar a tarefa (F12): HOURS ou DAYS.
 *
 * <p>Valor e unidade andam juntos: sem valor estimado a unidade tambem e
 * nula — quem garante isso e a entidade {@link Task}, nao questa API.
 * A analise/decomposicao de IA sempre propoe HOURS; a unidade so vira DAYS
 * pela escolha do usuario no formulario.
 */
public enum TimeUnit {
    HOURS,
    DAYS
}