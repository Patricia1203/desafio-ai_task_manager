package com.desafio.taskmanager.task.application.dto;

/**
 * Indicadores do dashboard (RF-20).
 *
 * <p>Record com nome em vez de {@code Map<String, Long>}: o contrato do
 * dashboard fica explicito no codigo e no JSON, e o teste quebra em vez de
 * devolver null silencioso se um campo for renomeado.
 *
 * @param total        tarefas de topo e subtarefas
 * @param pendentes    status TODO
 * @param emAndamento  status IN_PROGRESS
 * @param concluidas   status DONE
 * @param altaPrioridade quantidade de tarefas com prioridade HIGH
 */
public record TaskSummary(
        long total,
        long pendentes,
        long emAndamento,
        long concluidas,
        long altaPrioridade) {
}