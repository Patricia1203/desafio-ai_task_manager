package com.desafio.taskmanager.ai.adapter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.desafio.taskmanager.ai.adapter.config.AiProperties;
import com.desafio.taskmanager.ai.application.LlmResponseValidator;
import com.desafio.taskmanager.ai.port.TaskAiPort;
import com.desafio.taskmanager.ai.port.dto.TaskAiContext;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.common.error.LlmCommunicationException;
import com.desafio.taskmanager.common.error.LlmUnavailableException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClientException;

/**
 * Implementacao da {@link TaskAiPort} em cima do ChatClient do Spring AI — a
 * unica classe de dominio que conhece o provedor (RNF-14).
 *
 * <p>Cada caso de uso envia ao modelo somente os campos que o prompt usa
 * (RNF-11), truncados nos limites de {@code app.ai} antes de virarem variavel
 * do {@code .st}. A resposta sempre passa pelo {@link LlmResponseValidator}
 * (RNF-10): o {@code responseEntity(...)} do Spring AI devolve o record
 * parseado, mas nao rejeita campo desconhecido nem limite de dominio — quem
 * rejeita e o validador, sobre o JSON cru.
 *
 * <p>Retry controlado (design.md): disse saida invalida — JSON malformado,
 * enum fora do schema, campo faltando ou rejeicao do validador — refaz a
 * chamada com uma instrucao de correcao; esgotado {@code app.ai.max-retries}
 * lanca {@link InvalidLlmResponseException} (ERR-04). Falha de transporte nao
 * entra no retry: conexao recusada vira {@link LlmUnavailableException}
 * (ERR-05) e timeout/I/O vira {@link LlmCommunicationException} (ERR-03).
 *
 * <p>O mapeamento para resposta HTTP e da camada de api (T-F03-03); aqui as
 * excecoes apenas nascem tipadas.
 */
public class SpringAiTaskAiAdapter implements TaskAiPort {

    private static final String PROMPTS = "prompts/";

    private final ChatClient chatClient;
    private final LlmResponseValidator validator;
    private final AiProperties properties;

    public SpringAiTaskAiAdapter(ChatClient chatClient, LlmResponseValidator validator, AiProperties properties) {
        this.chatClient = chatClient;
        this.validator = validator;
        this.properties = properties;
    }

    /** RF-10: so titulo e descricao chegam ao prompt, truncados. */
    @Override
    public TaskImprovement improve(TaskAiContext context) {
        Map<String, Object> params = paramsDoContexto(context);
        params.put("maxTitleLength", properties.maxTitleLength());
        params.put("maxTextLength", properties.maxTextLength());
        return execute("task-improve.st", params, TaskImprovement.class, validator::validateImprovement);
    }

    /** RF-11: titulo, descricao e a prioridade atual (unica que o prompt usa). */
    @Override
    public TaskAnalysis analyze(TaskAiContext context) {
        Map<String, Object> params = paramsDoContexto(context);
        params.put("priority", context.priority().name());
        params.put("maxEstimatedHours", properties.maxEstimatedHours());
        params.put("maxTextLength", properties.maxTextLength());
        return execute("task-analyze.st", params, TaskAnalysis.class, validator::validateAnalysis);
    }

    /** RF-12: titulo, descricao e o intervalo de subtarefas. */
    @Override
    public TaskDecomposition decompose(TaskAiContext context) {
        Map<String, Object> params = paramsDoContexto(context);
        params.put("minSubtasks", properties.minSubtasks());
        params.put("maxSubtasks", properties.maxSubtasks());
        params.put("maxTitleLength", properties.maxTitleLength());
        params.put("maxTextLength", properties.maxTextLength());
        return execute("task-decompose.st", params, TaskDecomposition.class, validator::validateDecomposition);
    }

    private <T> T execute(
            String promptFile, Map<String, Object> params, Class<T> type, Function<String, T> validar) {
        int tentativas = properties.maxRetries() + 1;
        InvalidLlmResponseException ultimaFalha = null;
        String correcao = null;

        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                ChatClient.ChatClientRequestSpec requisicao = chatClient.prompt()
                        .user(spec -> spec
                                .text(new ClassPathResource(PROMPTS + promptFile), StandardCharsets.UTF_8)
                                .params(params));
                if (correcao != null) {
                    requisicao.messages(new SystemMessage(correcao));
                }
                ResponseEntity<ChatResponse, T> resposta = requisicao.call().responseEntity(type);
                return validar.apply(textoCru(resposta.getResponse()));
            } catch (InvalidLlmResponseException e) {
                ultimaFalha = e;
            } catch (RuntimeException e) {
                RuntimeException transporte = traduzirTransporte(e);
                if (transporte != null) {
                    throw transporte;
                }
                ultimaFalha = new InvalidLlmResponseException(
                        "saida estruturada invalida: " + e.getMessage(), e);
            }
            correcao = "Sua resposta anterior foi rejeitada: " + ultimaFalha.getMessage()
                    + ". Responda somente com o JSON no formato pedido, sem texto fora dele.";
        }
        throw new InvalidLlmResponseException(
                "resposta da IA invalida apos " + tentativas + " tentativas: " + ultimaFalha.getMessage(),
                ultimaFalha);
    }

    /** Conexao recusada ou host nao resolvido; o resto da cadeia I/O/HTTP vira falha de comunicacao. */
    private static RuntimeException traduzirTransporte(RuntimeException e) {
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

    /** JSON cru entregue ao validador; null se o provedor devolveu resposta sem conteudo. */
    private static String textoCru(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return null;
        }
        AssistantMessage output = response.getResult().getOutput();
        return output == null ? null : output.getText();
    }

    private Map<String, Object> paramsDoContexto(TaskAiContext context) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", truncar(context.title(), properties.maxTitleLength()));
        params.put("description", truncar(context.description(), properties.maxTextLength()));
        return params;
    }

    private static String truncar(String valor, int maximo) {
        if (valor == null) {
            return "";
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
