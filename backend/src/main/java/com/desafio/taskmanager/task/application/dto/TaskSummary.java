package com.desafio.taskmanager.task.application.dto;

/**
 * Indicadores do dashboard (RF-20).
 *
 * <p>Record com nome em vez de {@code Map<String, Long>}: o contrato do
 * dashboard fica explicito no codigo e no JSON, e o teste quebra em vez de
 * devolver null silencioso se um campo for renomeado.
 *
 * <p>Campos em ingles desde a revisao F06 (documentacao do desafio exige
 * ingles); os rotulos em portugues ficam so na UI.
 *
 * @param total        tarefas de topo e subtarefas
 * @param pending      status TODO
 * @param inProgress   status IN_PROGRESS
 * @param done         status DONE
 * @param highPriority tarefas com prioridade HIGH
 */
public record TaskSummary(
        long total,
        long pending,
        long inProgress,
        long done,
        long highPriority) {
}