package com.desafio.taskmanager.ai.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Um item da lista aceita pelo POST /decompose/apply (US-022, RF-14).
 *
 * <p>A validacao acontece na borda, por Bean Validation (CONVENTIONS): titulo
 * obrigatorio e nos limites do dominio, descricao opcional, e estimatedHours
 * — se o modelo trouxe — dentro de (0, 200], o mesmo intervalo que o
 * {@code LlmResponseValidator} exige da resposta da IA. Draft invalido vira
 * 400 com a lista {@code errors} apontando o item (ex.:
 * {@code subtasks[0].title}), antes de qualquer escrita.
 *
 * <p>{@code estimatedHours} nao e persistido: a tabela {@code tasks} nao tem
 * coluna de horas — o valor serve para o usuario conferir a proposta e e
 * descartado na conversao para {@code TaskCommand}.
 */
public record SubtaskDraft(

        @NotBlank(message = "titulo nao pode ser vazio")
        @Size(max = 200, message = "titulo deve ter no maximo 200 caracteres")
        String title,

        @Size(max = 5000, message = "descricao deve ter no maximo 5000 caracteres")
        String description,

        @DecimalMin(value = "0", inclusive = false, message = "horasEstimadas deve ser maior que zero")
        @DecimalMax(value = "200", message = "horasEstimadas deve ter no maximo 200")
        Double estimatedHours) {
}
