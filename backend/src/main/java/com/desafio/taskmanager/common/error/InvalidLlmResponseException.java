package com.desafio.taskmanager.common.error;

/**
 * Resposta do LLM fora do contrato (ERR-04): JSON malformado, enum inválido,
 * campo faltando, campo desconhecido ou limite de configuração estourado.
 *
 * <p>Nasce aqui com o validador (T-F03-01). O mapeamento para 502 com código
 * LLM_INVALID_RESPONSE é da camada de api (T-F03-03); até lá nenhuma rota a
 * lança, e se lançasse cairia na rede de segurança do
 * {@link GlobalExceptionHandler} como 500 genérico.
 */
public class InvalidLlmResponseException extends RuntimeException {

    public InvalidLlmResponseException(String message) {
        super(message);
    }

    public InvalidLlmResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
