package com.desafio.taskmanager.ai.adapter;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.desafio.taskmanager.assistant.application.tools.TaskQueryTools;
import com.desafio.taskmanager.assistant.domain.ChatRole;
import com.desafio.taskmanager.assistant.port.AssistantPort;
import com.desafio.taskmanager.common.config.AssistantLimitsProperties;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.json.JsonMapper;

/**
 * Implementacao da {@link AssistantPort} em cima do ChatClient do Spring AI -
 * a fronteira do provedor do assistente (RNF-14, mesmo lugar do
 * {@code SpringAiTaskAiAdapter}).
 *
 * <p>Monta o prompt de grounding (assistant-system.st) com a data atual
 * injetada pelo backend, encadeia o historico em ordem cronologica e registra
 * as ferramentas somente-leitura do {@link TaskQueryTools}.
 *
 * <p><b>Tool calling vs. fallback.</b> Com {@code app.assistant.tool-calling}
 * ligado (default), as ferramentas vao como tool calling reproduzido pelo
 * Spring AI (US-032). Com desligado - modelo que nao suporta - o adaptador
 * injeta no system prompt um contexto pre-montado (pendentes, vencidas e
 * indicadores) e nao registra tool alguma; a decisao de qual modo roda em
 * producao fica no STATE.md.
 *
 * <p>Resposta em texto livre: vazia e rejeitada como resposta fora do contrato
 * (ERR-04). Falha de transporte nao vira "resposta invalida": conexao recusada
 * vira {@code LlmUnavailableException} (ERR-05) e timeout/I/O vira
 * {@code LlmCommunicationException} (ERR-03), pelas mesmas regras do adaptador
 * de tarefas.
 */
public class SpringAiAssistantAdapter implements AssistantPort {

    private static final String PROMPTS = "prompts/";

    private final ChatClient chatClient;
    private final TaskQueryTools tools;
    private final boolean toolCalling;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();

    public SpringAiAssistantAdapter(
            ChatClient chatClient, TaskQueryTools tools, AssistantLimitsProperties properties, Clock clock) {
        this.chatClient = chatClient;
        this.tools = tools;
        this.toolCalling = properties.toolCalling();
        this.clock = clock;
    }

    @Override
    public String chat(List<Mensagem> historico, String mensagem) {
        Map<String, Object> params = new HashMap<>();
        params.put("currentDate", LocalDate.now(clock).toString());
        params.put("contextoOpcoes", toolCalling ? "" : contextoOpcoes());

        ChatClient.ChatClientRequestSpec requisicao = chatClient.prompt();
        requisicao.system(spec -> spec
                .text(new ClassPathResource(PROMPTS + "assistant-system.st"), StandardCharsets.UTF_8)
                .params(params));
        List<Message> historicoMessages = historico.stream()
                .map(SpringAiAssistantAdapter::mensagemDoHistorico)
                .toList();
        if (!historicoMessages.isEmpty()) {
            requisicao.messages(historicoMessages);
        }
        if (toolCalling) {
            requisicao.tools(new AssistantToolCallbacks(tools));
        }
        requisicao.user(mensagem);

        try {
            String resposta = requisicao.call().content();
            if (resposta == null || resposta.isBlank()) {
                throw new InvalidLlmResponseException("assistente devolveu resposta vazia");
            }
            return resposta;
        } catch (RuntimeException e) {
            RuntimeException transporte = SpringAiTransportErrors.traduzir(e);
            if (transporte != null) {
                throw transporte;
            }
            throw e;
        }
    }

    private static Message mensagemDoHistorico(Mensagem linha) {
        return linha.role() == ChatRole.USER
                ? new UserMessage(linha.content())
                : new AssistantMessage(linha.content());
    }

    /** Fallback sem tool calling: um retrato das leituras mais uteis ja pronto no prompt. */
    private String contextoOpcoes() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("pendentes", tools.getPendingTasks());
        dados.put("vencidas", tools.getOverdueTasks());
        dados.put("indicadores", tools.getTaskSummary());
        return "\n- Contexto de tarefas disponivel (retrato):\n" + json.writeValueAsString(dados);
    }
}