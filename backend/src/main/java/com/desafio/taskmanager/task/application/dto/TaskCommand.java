package com.desafio.taskmanager.task.application.dto;

import java.time.LocalDate;

import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TimeUnit;

/**
 * Entrada neutra de criacao ou edicao de tarefa (RNF-20).
 *
 * <p>Existe para que {@code task.application} nao conheca os DTOs da API: o
 * controller converte o request em comando, o service converte o comando em
 * {@code Task}. A validacao de contrato (campos obrigatorios, tamanhos) continua
 * nos records de task.api.dto; as regras de negocio (titulo nao vazio, trim,
 * prioridade default) continuam na entidade.
 *
 * @param titulo        titulo informado, sem normalizacao aqui — o dominio aparada
 * @param descricao     descricao opcional
 * @param prioridade    nula significa "usar a prioridade default"
 * @param prazo         prazo em data, opcional
 * @param tempoEstimado F12: tempo estimado para realizar, opcional; o dominio
 *                      assume HOURS quando a unidade vem nula
 * @param unidadeTempo  F12: unidade do tempo estimado, opcional
 */
public record TaskCommand(
        String titulo,
        String descricao,
        TaskPriority prioridade,
        LocalDate prazo,
        Double tempoEstimado,
        TimeUnit unidadeTempo) {

    /** Variante sem tempo estimado, usada por quem ainda nao conhece F12. */
    public TaskCommand(String titulo, String descricao, TaskPriority prioridade, LocalDate prazo) {
        this(titulo, descricao, prioridade, prazo, null, null);
    }
}