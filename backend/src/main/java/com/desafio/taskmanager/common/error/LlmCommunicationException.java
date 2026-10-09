package com.desafio.taskmanager.common.error;

/**
 * Falha de comunicacao com o provedor de IA (ERR-03): timeout, erro de I/O
 * ou resposta HTTP inesperada do Ollama. O adaptador traduz a excecao de
 * transporte do Spring AI sem repassar detalhe de conexao ao chamador;
 * mapeada para 502.
 */
public class LlmCommunicationException extends RuntimeException {

    public LlmCommunicationException(String message) {
        super(message);
    }

    public LlmCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
