package com.desafio.taskmanager.ai.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /api/ai/tasks/{id}/decompose/apply (US-022): as subtarefas que
 * o usuario aceitou da sugestao da IA. A aplicacao cria exatamente esta lista,
 * cada uma apontando para a tarefa do id do caminho (RF-14).
 *
 * <p>A lista e obrigatoria (aplicar nada e erro de quem chama) e limitada ao
 * maximo de subtarefas que a IA pode propor (10): a sugestao nunca traz mais
 * do que isso, e um limite impede escrita ilimitada por request.
 */
public record ApplyDecompositionRequest(

        @NotEmpty(message = "subtarefas nao pode ser vazia")
        @Size(max = 10, message = "subtarefas deve ter no maximo 10 itens")
        List<@Valid SubtaskDraft> subtasks) {
}
