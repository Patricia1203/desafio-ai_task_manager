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
 * @param total        itens finais: toda linha que nao tem subtarefa (se a
 *                     tarefa tem filhas, quem conta sao as filhas)
 * @param pending      status TODO entre os itens finais
 * @param inProgress   status IN_PROGRESS entre os itens finais
 * @param done         status DONE entre os itens finais
 * @param highPriority itens finais com prioridade HIGH ainda nao concluidos
 */
public record TaskSummary(
        long total,
        long pending,
        long inProgress,
        long done,
        long highPriority) {
}