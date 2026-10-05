package com.desafio.taskmanager.common.error;

import java.util.List;

import org.springframework.validation.FieldError;

/**
 * Corpo de erro de validacao, anexado ao ProblemDetail sob a propriedade "errors".
 * Cada item aponta o campo e a mensagem, sem expor valores enviados pelo cliente.
 *
 * @param field  nome do campo invalido (em notacao de ponto para aninhados)
 * @param reason descricao legivel do problema
 */
public record FieldErrorItem(String field, String reason) {

    public static List<FieldErrorItem> from(List<FieldError> errors) {
        return errors.stream()
                .map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();
    }
}