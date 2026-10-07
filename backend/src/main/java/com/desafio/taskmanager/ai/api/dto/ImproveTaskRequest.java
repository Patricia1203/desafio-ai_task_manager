package com.desafio.taskmanager.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /api/ai/tasks/improve (US-020): a sugestao de melhoria pede o
 * conteudo a melhorar, nao o id — quem chama ja tem a tarefa aberta e decide
 * depois se aplica a sugestao via PUT /api/tasks/{id} (RF-10).
 *
 * <p>Os nomes dos campos estao em ingles (RNF-10); a record
 * {@code TaskImprovement} da porta continua em ingles, porque e contrato
 * interno.
 */
public record ImproveTaskRequest(

        @NotBlank(message = "titulo e obrigatorio")
        @Size(max = 200, message = "titulo deve ter no maximo 200 caracteres")
        String title,

        @Size(max = 5000, message = "descricao deve ter no maximo 5000 caracteres")
        String description) {
}
