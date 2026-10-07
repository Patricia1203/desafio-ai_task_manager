package com.desafio.taskmanager.assistant.port;

import java.util.List;

import com.desafio.taskmanager.assistant.domain.ChatRole;

/**
 * Porta de saida do assistente (F04). A camada de aplicacao fala com esta
 * interface; quem importa Spring AI e so o adaptador em {@code ai.adapter}
 * (RNF-14, mesmo padrao da {@code TaskAiPort} da F03).
 *
 * <p>O contrato e o minimo que o caso de uso possui: o historico em ordem
 * cronologica (mais antiga primeiro), a mensagem atual do usuario e a resposta
 * em texto livre do modelo. A data atual, as ferramentas somente-leitura e o
 * prompt de grounding sao decisoes do adaptador - o service nao conhece o
 * provedor nem o {@code .st}.
 *
 * <p>O {@code chat()} nunca devolve texto vazio: resposta em branco do modelo
 * e falha mapeada ({@code InvalidLlmResponseException}, ERR-04).
 */
public interface AssistantPort {

    /**
     * Um turno do historico, na ordem em que foi falado. {@link ChatRole} e o
     * contrato interno da tabela {@code chat_messages}, nao o JSON publico.
     */
    record Mensagem(ChatRole role, String content) {
    }

    /** Resposta do assistente para {@code mensagem} com o {@code historico} como contexto. */
    String chat(List<Mensagem> historico, String mensagem);
}