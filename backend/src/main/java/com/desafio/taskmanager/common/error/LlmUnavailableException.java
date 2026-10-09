package com.desafio.taskmanager.common.error;

/**
 * Provedor de IA fora do ar (ERR-05): conexao recusada, host nao resolvido
 * ou Ollama parado. Mapeada para 503 (repetir depois), ao contrario de
 * {@link LlmCommunicationException}, cuja falha de comunicacao pede 502.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
