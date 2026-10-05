package com.desafio.taskmanager.task.application.dto;

/**
 * Indicadores do dashboard (RF-20).
 *
 * <p>Record com nome em vez de {@code Map<String, Long>}: o contrato do
 * dashboard fica explicito no codigo e no JSON, e o teste quebra em vez de
 * devolver null silencioso se um campo for renomeado.
 *
 * @param total          tarefas de topo e subtarefas
 * @param pendentes      status A_FAZER
 * @param emAndamento    status EM_ANDAMENTO
 * @param concluidas     status CONCLUIDA
 * @param altaPrioridade tarefas com prioridade ALTA
 */
public record TaskSummary(
        long total,
        long pendentes,
        long emAndamento,
        long concluidas,
        long altaPrioridade) {
}