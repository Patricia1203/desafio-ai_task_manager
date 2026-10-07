package com.desafio.taskmanager.ai.adapter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import com.desafio.taskmanager.common.error.LlmCommunicationException;
import com.desafio.taskmanager.common.error.LlmUnavailableException;

import org.springframework.web.client.RestClientException;

/**
 * Traducao das excecoes de transporte do Spring AI para os tipos que a camada
 * de api mapeia (ERR-03/ERR-05): conexao recusada ou host nao resolvido vira
 * {@link LlmUnavailableException} (503), e o resto da cadeia I/O/HTTP vira
 * {@link LlmCommunicationException} (502). Compartilhado pelo adaptador de
 * tarefas (F03) e pelo do assistente (F04); o FalhaDeTransporte dos testes
 * prova que a causa raiz chega preservada.
 */
final class SpringAiTransportErrors {

    private SpringAiTransportErrors() {
    }

    /** null quando a excecao nao e transporte (a chamada segue e o erro original propaga). */
    static RuntimeException traduzir(RuntimeException e) {
        if (causaTem(e, ConnectException.class) || causaTem(e, UnknownHostException.class)) {
            return new LlmUnavailableException("LLM indisponivel: " + causaRaiz(e), e);
        }
        if (causaTem(e, IOException.class) || causaTem(e, RestClientException.class)) {
            return new LlmCommunicationException("falha de comunicacao com o LLM: " + causaRaiz(e), e);
        }
        return null;
    }

    private static boolean causaTem(Throwable e, Class<? extends Throwable> tipo) {
        Throwable atual = e;
        while (atual != null) {
            if (tipo.isInstance(atual)) {
                return true;
            }
            atual = atual.getCause() == atual ? null : atual.getCause();
        }
        return false;
    }

    private static String causaRaiz(Throwable e) {
        Throwable atual = e;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual.getMessage() == null ? atual.getClass().getSimpleName() : atual.getMessage();
    }
}