package com.desafio.taskmanager.assistant.application.tools.dto;

import java.util.List;

/**
 * Envelope das ferramentas de lista do assistente (T-F06-07).
 *
 * <p>Antes a ferramenta devolvia so a lista ja cortada em
 * {@code app.assistant.max-tool-results}: o modelo respondia "sao 3 pendentes"
 * quando havia 40, porque nunca via o total. Aqui o {@code total} e o numero
 * real do filtro e {@code itens} e o recorte, entao a resposta pode dizer
 * "3 de 40" em vez de mentir por omissao.
 *
 * <p>O nome do envelope e {@code itens} porque e o contrato escrito da task e do
 * design da F06; os campos de {@link TaskToolResult} seguem em ingles.
 */
public record ToolResultPage(long total, List<TaskToolResult> itens) {

    public ToolResultPage {
        itens = List.copyOf(itens);
    }
}