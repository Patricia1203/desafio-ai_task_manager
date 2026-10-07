package com.desafio.taskmanager.ai.adapter;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.stream.Collectors;

import com.desafio.taskmanager.ai.adapter.config.AiProperties;
import com.desafio.taskmanager.ai.application.LlmResponseValidator;
import com.desafio.taskmanager.ai.port.dto.TaskAiContext;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskComplexity;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.common.error.LlmCommunicationException;
import com.desafio.taskmanager.common.error.LlmUnavailableException;
import com.desafio.taskmanager.task.domain.TaskPriority;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TST-03 + criterios de "Pronto quando" da T-F03-02: o adaptador e a fronteira
 * entre a porta e o Spring AI, entao cada promessa da task tem um caso aqui —
 * caso feliz validado, retry com instrucao de correcao e esgotamento em
 * InvalidLlmResponseException, traducao de timeout/conexao para as excecoes
 * mapeadas e prompt contendo somente os campos do contexto, truncados.
 *
 * <p>Sem Spring e sem Ollama: o ChatModel e o {@link FakeChatModelSupport}
 * roteirizado, que tambem guarda os prompts — as assercoes de "o que chegou ao
 * modelo" saem da captura, nao de mock solto.
 */
@DisplayName("SpringAiTaskAiAdapter — porta, retry e mapeamento de falhas")
class SpringAiTaskAiAdapterTest {

    private final AiProperties properties = new AiProperties(Duration.ofSeconds(60), 1, 2, 10, 200, 200, 5000);
    private final FakeChatModelSupport fake = new FakeChatModelSupport();
    private final TaskAiContext contexto =
            new TaskAiContext("Preparar relatorio", "Relatorio trimestral para a diretoria", TaskPriority.MEDIUM);
    private final SpringAiTaskAiAdapter adapter = adaptador();

    private SpringAiTaskAiAdapter adaptador() {
        ChatClient chatClient = ChatClient.builder(fake.chatModel())
                .defaultTemplateRenderer(StTemplateRenderer.builder().build())
                .build();
        return new SpringAiTaskAiAdapter(
                chatClient, new LlmResponseValidator(2, 10, 200, 200, 5000), properties);
    }

    @Nested
    @DisplayName("caso feliz devolve o record validado")
    class CasoFeliz {

        @Test
        @DisplayName("melhoria devolve titulo e descricao com o prompt montado e formatado")
        void melhoriaDevolveRecordValidado() {
            fake.respond("""
                    {"title": "Relatorio trimestral", "description": "Entregar ate sexta"}
                    """);

            TaskImprovement resultado = adapter.improve(contexto);

            assertThat(resultado.title()).isEqualTo("Relatorio trimestral");
            assertThat(resultado.description()).isEqualTo("Entregar ate sexta");
            assertThat(fake.prompts()).hasSize(1);
            assertThat(textoDoPrompt(fake.prompts().get(0)))
                    .contains("Título: Preparar relatorio")
                    .contains("Your response should be in JSON format");
        }

        @Test
        @DisplayName("analise devolve os enums do schema e leva a prioridade atual no prompt")
        void analiseDevolveRecord() {
            fake.respond("""
                    {"priority": "HIGH", "complexity": "MEDIUM", "estimatedHours": 12, "reason": "Prazo curto"}
                    """);

            TaskAnalysis resultado = adapter.analyze(contexto);

            assertThat(resultado.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(resultado.complexity()).isEqualTo(TaskComplexity.MEDIUM);
            assertThat(resultado.estimatedHours()).isEqualTo(12.0);
            assertThat(fake.prompts()).hasSize(1);
            assertThat(textoDoPrompt(fake.prompts().get(0)))
                    .contains("Prioridade atual: MEDIUM")
                    .contains("Your response should be in JSON format");
        }

        @Test
        @DisplayName("decomposicao devolve a lista validada com o intervalo no prompt")
        void decomposicaoDevolveLista() {
            fake.respond("""
                    {"subtasks": [
                      {"title": "Levantar numeros", "description": "Coletar dados", "estimatedHours": 4},
                      {"title": "Revisar texto", "description": "Checar erros", "estimatedHours": 2}
                    ]}
                    """);

            TaskDecomposition resultado = adapter.decompose(contexto);

            assertThat(resultado.subtasks()).hasSize(2);
            assertThat(resultado.subtasks().get(0).title()).isEqualTo("Levantar numeros");
            assertThat(fake.prompts()).hasSize(1);
            assertThat(textoDoPrompt(fake.prompts().get(0))).contains("entre 2 e 10 no total");
        }
    }

    @Nested
    @DisplayName("o prompt leva so o contexto permitido (RNF-11)")
    class ContextoMinimo {

        @Test
        @DisplayName("melhoria nao leva prioridade nem nenhum campo alem de titulo e descricao")
        void somenteCamposDoContexto() {
            TaskAiContext comPrioridadeAlta =
                    new TaskAiContext("Preparar relatorio", "Relatorio trimestral", TaskPriority.HIGH);
            fake.respond("""
                    {"title": "ok", "description": "ok"}
                    """);

            adapter.improve(comPrioridadeAlta);

            assertThat(textoDoPrompt(fake.prompts().get(0)))
                    .doesNotContain("HIGH")
                    .doesNotContain("Prioridade");
        }

        @Test
        @DisplayName("titulo e descricao longos sao truncados nos limites de app.ai")
        void truncamentoNosLimites() {
            TaskAiContext longo = new TaskAiContext("a".repeat(250), "b".repeat(5100), TaskPriority.LOW);
            fake.respond("""
                    {"title": "curto", "description": "curta"}
                    """);

            adapter.improve(longo);

            assertThat(textoDoPrompt(fake.prompts().get(0)))
                    .contains("a".repeat(200))
                    .doesNotContain("a".repeat(201))
                    .contains("b".repeat(5000))
                    .doesNotContain("b".repeat(5001));
        }
    }

    @Nested
    @DisplayName("saida invalida dispara retry e depois InvalidLlmResponseException")
    class SaidaInvalida {

        @Test
        @DisplayName("JSON invalido na primeira chamada vai para a segunda com correcao e ai passa")
        void jsonInvalidoDisparaRetryEDepoisSucesso() {
            fake.respond("isso nao e json", """
                    {"title": "Melhor titulo", "description": "Melhor descricao"}
                    """);

            TaskImprovement resultado = adapter.improve(contexto);

            assertThat(resultado.title()).isEqualTo("Melhor titulo");
            assertThat(fake.prompts()).hasSize(2);
            assertThat(textoDoPrompt(fake.prompts().get(0))).doesNotContain("rejeitada");
            assertThat(textoDoPrompt(fake.prompts().get(1))).contains("rejeitada");
        }

        @Test
        @DisplayName("JSON invalido sem sucesso esgota o retry com exatamente duas chamadas")
        void jsonInvalidoEsgotaRetry() {
            fake.respond("isso nao e json", "tambem nao e json");

            assertThatThrownBy(() -> adapter.improve(contexto))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("apos 2 tentativas");
            assertThat(fake.prompts()).hasSize(2);
        }

        @Test
        @DisplayName("enum fora do schema vira retry e depois InvalidLlmResponseException")
        void enumInvalidoDisparaRetry() {
            String comEnumInvalido = """
                    {"priority": "HIGH", "complexity": "EXTREME", "estimatedHours": 8, "reason": "prazo curto"}
                    """;
            fake.respond(comEnumInvalido, comEnumInvalido);

            assertThatThrownBy(() -> adapter.analyze(contexto))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("apos 2 tentativas");
            assertThat(fake.prompts()).hasSize(2);
        }

        @Test
        @DisplayName("campo faltando passa pelo validador, dispara retry e depois lanca")
        void campoFaltandoDisparaRetry() {
            String semHoras = """
                    {"priority": "MEDIUM", "complexity": "LOW", "reason": "sem base para estimar"}
                    """;
            fake.respond(semHoras, semHoras);

            assertThatThrownBy(() -> adapter.analyze(contexto))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("estimatedHours");
            assertThat(fake.prompts()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("falha de transporte nao entra no retry e vira a excecao mapeada")
    class FalhaDeTransporte {

        @Test
        @DisplayName("erro de I/O vira LlmCommunicationException com a causa preservada")
        void erroDeIoViraComunicacao() {
            fake.fail(new ResourceAccessException("falha na leitura", new IOException("broken pipe")));

            assertThatThrownBy(() -> adapter.improve(contexto))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining("broken pipe");
            assertThat(fake.prompts()).hasSize(1);
        }

        @Test
        @DisplayName("timeout de leitura vira LlmCommunicationException")
        void timeoutViraComunicacao() {
            fake.fail(new ResourceAccessException("timeout", new SocketTimeoutException("Read timed out")));

            assertThatThrownBy(() -> adapter.improve(contexto))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining("Read timed out");
            assertThat(fake.prompts()).hasSize(1);
        }

        @Test
        @DisplayName("conexao recusada vira LlmUnavailableException")
        void conexaoRecusadaViraIndisponivel() {
            fake.fail(new ResourceAccessException("conexao recusada", new ConnectException("Connection refused")));

            assertThatThrownBy(() -> adapter.improve(contexto))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("Connection refused");
            assertThat(fake.prompts()).hasSize(1);
        }
    }

    /** Texto integral do prompt (system + user): e dele que sai a correcao e o .st renderizado. */
    private static String textoDoPrompt(Prompt prompt) {
        return prompt.getInstructions().stream()
                .map(Message::getText)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
