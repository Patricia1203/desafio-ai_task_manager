package com.desafio.taskmanager.ai.port.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lista de subtarefas propostas (RF-12). A quantidade é validada contra
 * {@code app.ai.min-subtasks}/{@code app.ai.max-subtasks} antes de sair do
 * validador; criar ou não cada item é decisão da aplicação (RF-14).
 */
public record TaskDecomposition(
        @JsonProperty("subtasks") List<ProposedSubtask> subtasks) {
}
