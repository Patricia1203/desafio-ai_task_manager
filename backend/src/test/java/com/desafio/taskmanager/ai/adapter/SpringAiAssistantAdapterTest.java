package com.desafio.taskmanager.ai.adapter;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.desafio.taskmanager.assistant.application.tools.TaskQueryTools;
import com.desafio.taskmanager.assistant.domain.ChatRole;
import com.desafio.taskmanager.assistant.port.AssistantPort;
import com.desafio.taskmanager.common.config.AssistantLimitsProperties;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.common.error.LlmCommunicationException;
import com.desafio.taskmanager.common.error.LlmUnavailableException;
import com.desafio.taskmanager.task.infra.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Adaptador do assistente contra o ChatModel falso roteirizado (mesmo padrao
 * do SpringAiTaskAiAdapterTest, sem Spring e sem Ollama): grounding com a data
 * atual, historico em ordem, tool calling registrado quando habilitado,
 * contexto pre-montado quando desabilitado e a traducao das falhas de
 * transporte (ERR-03/ERR-05).
 */
@DisplayName("SpringAiAssistantAdapter - grounding, historico, tools e falhas")
class SpringAiAssistantAdapterTest {

    private final FakeChatModelSupport fake = new FakeChatModelSupport();
    private TaskRepository repository;

    @BeforeEach
    void setStubs() {
        repository = mock(TaskRepository.class);
        when(repository.findByStatusInOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repository.countAll()).thenReturn(0L);
        when(repository.countByStatusValue(any())).thenReturn(0L);
        when(repository.countByPriorityValue(any())).thenReturn(0L);
    }

    private SpringAiAssistantAdapter adaptador(boolean toolCalling) {
        ChatClient chatClient = ChatClient.builder(fake.chatModel())
                .defaultTemplateRenderer(StTemplateRenderer.builder().build())
                .build();
        return new SpringAiAssistantAdapter(chatClient, ferramentas(), new AssistantLimitsProperties(3, toolCalling));
    }

    private TaskQueryTools ferramentas() {
        return new TaskQueryTools(repository, new AssistantLimitsProperties(3, true));
    }

    private static List<AssistantPort.Mensagem> historico() {
        return List.of(
                new AssistantPort.Mensagem(ChatRole.USER, "quais estao pendentes?"),
                new AssistantPort.Mensagem(ChatRole.ASSISTANT, "duas tarefas em aberto"));
    }

    @Nested
    @DisplayName("com tool calling ligado (default)")
    class ComToolCalling {

        @Test
        @DisplayName("prompt de grounding com data atual, historico em ordem, pergunta e as seis ferramentas")
        void enviaGroundingHistoricoEAsSeisFerramentas() {
            fake.respond("Resposta com base nos dados");
            SpringAiAssistantAdapter adapter = adaptador(true);

            String resposta = adapter.chat(historico(), "e as vencidas?");

            assertThat(resposta).isEqualTo("Resposta com base nos dados");
            String texto = textoDoPrompt(fake.prompts().get(0));
            assertThat(texto)
                    .contains("Data de hoje: " + LocalDate.now())
                    .contains("quais estao pendentes?")
                    .contains("duas tarefas em aberto")
                    .contains("e as vencidas?")
                    .contains("Baseie a resposta APENAS nos dados");
            assertThat(texto.indexOf("quais estao pendentes?"))
                    .isLessThan(texto.indexOf("duas tarefas em aberto"))
                    .isLessThan(texto.indexOf("e as vencidas?"));
        }

        @Test
        @DisplayName("o wrapper registra as seis ferramentas somente-leitura pelo nome")
        void invOlucroExpoeAsSeisFerramentas() {
            // ToolCallbacks.from devolve array; Arrays converte para afirmar sobre os nomes
            List<String> nomes = Arrays.stream(ToolCallbacks.from(new AssistantToolCallbacks(ferramentas())))
                    .map(callback -> callback.getToolDefinition().name())
                    .toList();

            assertThat(nomes).containsExactlyInAnyOrder(
                    "get_pending_tasks", "get_overdue_tasks", "get_task_by_id",
                    "get_tasks_by_priority", "get_tasks_due_soon", "get_task_summary");
        }
    }

    @Nested
    @DisplayName("com tool calling desligado (fallback)")
    class SemToolCalling {

        @Test
        @DisplayName("contexto pre-montado entra no prompt e nenhuma ferramenta e registrada")
        void injetaContextoESemFerramentas() {
            fake.respond("Resposta so com o contexto");
            SpringAiAssistantAdapter adapter = adaptador(false);

            String resposta = adapter.chat(List.of(), "resuma as pendentes");

            assertThat(resposta).isEqualTo("Resposta so com o contexto");
            assertThat(textoDoPrompt(fake.prompts().get(0)))
                    .contains("Contexto de tarefas disponivel")
                    .contains("\"pendentes\"");
            assertThat(fake.prompts().get(0).getOptions())
                    .isNotInstanceOf(ToolCallingChatOptions.class);
        }
    }

    @Nested
    @DisplayName("saida e transporte")
    class Falhas {

        @Test
        @DisplayName("resposta vazia do modelo vira InvalidLlmResponseException")
        void respostaVaziaViraRespostaInvalida() {
            fake.respond("   ");
            SpringAiAssistantAdapter adapter = adaptador(true);

            assertThatThrownBy(() -> adapter.chat(List.of(), "oi"))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("vazia");
        }

        @Test
        @DisplayName("timeout de leitura vira LlmCommunicationException")
        void timeoutViraComunicacao() {
            fake.fail(new ResourceAccessException("timeout", new SocketTimeoutException("Read timed out")));
            SpringAiAssistantAdapter adapter = adaptador(true);

            assertThatThrownBy(() -> adapter.chat(List.of(), "oi"))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining("Read timed out");
        }

        @Test
        @DisplayName("conexao recusada vira LlmUnavailableException")
        void conexaoRecusadaViraIndisponivel() {
            fake.fail(new ResourceAccessException("conexao recusada", new ConnectException("Connection refused")));
            SpringAiAssistantAdapter adapter = adaptador(true);

            assertThatThrownBy(() -> adapter.chat(List.of(), "oi"))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("Connection refused");
        }
    }

    /** Texto integral do prompt: o .st renderizado mais o historico e a pergunta. */
    private static String textoDoPrompt(Prompt prompt) {
        return prompt.getInstructions().stream()
                .map(Message::getText)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}