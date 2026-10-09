package com.desafio.taskmanager.ai.adapter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * ChatModel falso roteirizado: devolve as respostas e lancas as excecoes na
 * ordem em que forem cadastradas, e guarda cada {@link Prompt} recebido. E o
 * que permite ao SpringAiTaskAiAdapterTest afirmar o que chegou ao modelo —
 * prompt, quantidade de chamadas e traducao de falhas — sem Ollama, sem rede
 * e sem mock do Spring AI.
 */
final class FakeChatModelSupport {

    private final Deque<Object> passos = new ArrayDeque<>();
    private final List<Prompt> prompts = new ArrayList<>();

    FakeChatModelSupport respond(String... json) {
        for (String texto : json) {
            passos.add(new ChatResponse(List.of(new Generation(new AssistantMessage(texto)))));
        }
        return this;
    }

    FakeChatModelSupport fail(RuntimeException excecao) {
        passos.add(excecao);
        return this;
    }

    ChatModel chatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt);
                if (passos.isEmpty()) {
                    throw new IllegalStateException("FakeChatModel sem passo roteirizado");
                }
                Object passo = passos.poll();
                if (passo instanceof RuntimeException excecao) {
                    throw excecao;
                }
                return (ChatResponse) passo;
            }
        };
    }

    /** Prompts recebidos, na ordem das chamadas — o numero de itens e o numero de tentativas. */
    List<Prompt> prompts() {
        return prompts;
    }
}
