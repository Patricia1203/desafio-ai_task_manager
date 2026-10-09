package com.desafio.taskmanager.common.error;

/**
 * Resposta do LLM fora do contrato (ERR-04): JSON malformado, enum inválido,
 * campo faltando, campo desconhecido ou limite de configuração estourado.
 * Mapeada para 502 com o código LLM_INVALID_RESPONSE (camada de api).
 */
public class InvalidLlmResponseException extends RuntimeException {

    public InvalidLlmResponseException(String message) {
        super(message);
    }

    public InvalidLlmResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
