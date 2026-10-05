package com.desafio.taskmanager.task.api.dto;

import java.time.LocalDate;

import com.desafio.taskmanager.task.domain.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /api/tasks.
 *
 * <p>As mensagens das constraints vao para o ProblemDetail (propriedade
 * {@code errors}) e sao lidas pelo usuario, entao vao em portugues. Os nomes dos
 * componentes tambem: sao os campos do JSON (decisao do usuario).
 */
public record CreateTaskRequest(

        @NotBlank(message = "titulo e obrigatorio")
        @Size(max = 200, message = "titulo deve ter no maximo 200 caracteres")
        String titulo,

        @Size(max = 5000, message = "descricao deve ter no maximo 5000 caracteres")
        String descricao,

        /** Opcional: sem valor a tarefa nasce MEDIA. */
        TaskPriority prioridade,

        /** Prazo em data (sem hora). Opcional. */
        LocalDate prazo) {

    /** Titulo ja normalizado, para a camada de aplicacao nao repetir o trim. */
    public String tituloNormalizado() {
        return titulo == null ? null : titulo.trim();
    }
}
