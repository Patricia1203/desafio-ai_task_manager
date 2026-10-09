package com.desafio.taskmanager.ai.port.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sugestão de melhoria de uma tarefa (RF-10): só título e descrição; a IA
 * nunca persiste — aplicar ou não é decisão de quem chama.
 *
 * <p>{@code @JsonProperty} fixa os nomes do JSON Schema que o structured
 * output gera (RNF-10), estáveis mesmo que o componente Java mude de nome.
 * Esta record é contrato interno da porta, não o JSON que a API expõe
 * (RF-24).
 */
public record TaskImprovement(
        @JsonProperty("title") String title,
        @JsonProperty("description") String description) {
}
