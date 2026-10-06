package com.desafio.taskmanager.ai.port.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Uma subtarefa proposta pela decomposição (RF-12). {@code estimatedHours} é
 * opcional no schema (design.md): o modelo pode omitir; se mandar, tem que
 * estar dentro do intervalo — o validador cobre os dois casos.
 */
public record ProposedSubtask(
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("estimatedHours") Double estimatedHours) {
}
