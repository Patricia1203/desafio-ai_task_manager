package com.desafio.taskmanager.common.error;

/**
 * Falha de comunicacao com o provedor de IA (ERR-03): timeout, erro de I/O
 * ou resposta HTTP inesperada do Ollama.
 *
 * <p>Nasce no adaptador (T-F03-02), que traduz a excecao de transporte do
 * Spring AI sem repassar detalhe de conexao ao chamador. O mapeamento para
 * 502 e da camada de api (T-F03-03); ate la nenhuma rota a lanca, e se
 * lancasse cairia na rede de seguranca do {@link GlobalExceptionHandler}
 * como 500 generico.
 */
public class LlmCommunicationException extends RuntimeException {

    public LlmCommunicationException(String message) {
        super(message);
    }

    public LlmCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
