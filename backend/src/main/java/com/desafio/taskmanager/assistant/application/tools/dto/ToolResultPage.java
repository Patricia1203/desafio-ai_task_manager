package com.desafio.taskmanager.assistant.application.tools.dto;

import java.util.List;

/**
 * Envelope das ferramentas de lista do assistente (T-F06-07).
 *
 * <p>Antes a ferramenta devolvia so a lista ja cortada em
 * {@code app.assistant.max-tool-results}: o modelo respondia "sao 3 pendentes"
 * quando havia 40, porque nunca via o total. Aqui o {@code total} e o numero
 * real do filtro e {@code items} e o recorte, entao a resposta pode dizer
 * "3 de 40" em vez de mentir por omissao.
 *
 * <p>Os dois campos ficam em ingles para acompanhar {@link TaskToolResult}, que
 * esta em ingles desde a T-F06-04.
 */
public record ToolResultPage(long total, List<TaskToolResult> items) {

    public ToolResultPage {
        items = List.copyOf(items);
    }
}