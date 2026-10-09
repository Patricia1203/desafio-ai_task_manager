package com.desafio.taskmanager.ai.application;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.desafio.taskmanager.ai.adapter.config.AiProperties;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskComplexity;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.task.domain.TaskPriority;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TST-01 + critérios de "Pronto quando" da T-F03-01: o validador é a única
 * fronteira entre o JSON livre do modelo e o resto da aplicação, então cada
 * rejeição prometida tem um caso aqui — caso válido, enum inválido, horas fora
 * do intervalo, textos vazios ou longos demais e subtarefas duplicadas,
 * vazias ou excedentes.
 *
 * <p>Sem Spring e sem container: o validador é construído direto a partir do
 * {@link AiProperties} com os mesmos valores do application.yml, para o
 * teste valer como contrato e provar o binding pelas properties (T-F06-14).
 */
@DisplayName("LlmResponseValidator — so record tipado passa adiante")
class LlmResponseValidatorTest {

    private final AiProperties properties =
            new AiProperties(Duration.ofSeconds(180), 1, 2, 10, 200, 200, 5000);
    private final LlmResponseValidator validator = new LlmResponseValidator(properties);

    @Nested
    @DisplayName("melhoria (RF-10)")
    class Melhoria {

        @Test
        @DisplayName("JSON valido devolve o record com titulo e descricao")
        void jsonValidoEhAceito() {
            String json = """
                    {
                      "title": "Preparar relatorio trimestral para a diretoria",
                      "description": "Entregar o relatorio consolidado ate sexta com os numeros do trimestre"
                    }
                    """;

            TaskImprovement result = validator.validateImprovement(json);

            assertThat(result.title()).isEqualTo("Preparar relatorio trimestral para a diretoria");
            assertThat(result.description()).startsWith("Entregar o relatorio consolidado");
        }

        @Test
        @DisplayName("titulo em branco e rejeitado")
        void tituloEmBrancoEhRejeitado() {
            String json = """
                    {"title": "   ", "description": "descricao valida"}
                    """;

            assertThatThrownBy(() -> validator.validateImprovement(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("campo 'title' vazio");
        }

        @Test
        @DisplayName("titulo com 201 caracteres e rejeitado, o limite e 200")
        void tituloAcimaDoLimiteEhRejeitado() {
            String json = "{\"title\": \"" + "a".repeat(201) + "\", \"description\": \"ok\"}";

            assertThatThrownBy(() -> validator.validateImprovement(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("acima do limite de 200");
        }

        @Test
        @DisplayName("campo fora do schema e rejeitado, nao ignorado em silencio")
        void campoDesconhecidoEhRejeitado() {
            String json = """
                    {
                      "title": "titulo valido",
                      "description": "descricao valida",
                      "priority": "HIGH"
                    }
                    """;

            assertThatThrownBy(() -> validator.validateImprovement(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("priority");
        }
    }

    @Nested
    @DisplayName("analise (RF-11)")
    class Analise {

        @Test
        @DisplayName("analise valida devolve enums, horas e motivo")
        void analiseValidaEhAceita() {
            String json = """
                    {
                      "priority": "HIGH",
                      "complexity": "MEDIUM",
                      "estimatedHours": 8.5,
                      "reason": "Prazo curto e depende de dados de tres times"
                    }
                    """;

            TaskAnalysis result = validator.validateAnalysis(json);

            assertThat(result.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.complexity()).isEqualTo(TaskComplexity.MEDIUM);
            assertThat(result.estimatedHours()).isEqualTo(8.5);
            assertThat(result.reason()).contains("tres times");
        }

        @Test
        @DisplayName("complexidade fora do contrato e rejeitada no parse")
        void complexidadeInvalidaEhRejeitada() {
            String json = """
                    {
                      "priority": "HIGH",
                      "complexity": "EXTREME",
                      "estimatedHours": 8,
                      "reason": "motivo"
                    }
                    """;

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("analise");
        }

        @Test
        @DisplayName("prioridade fora do enum do dominio e rejeitada no parse")
        void prioridadeInvalidaEhRejeitada() {
            String json = """
                    {
                      "priority": "URGENTE",
                      "complexity": "LOW",
                      "estimatedHours": 2,
                      "reason": "motivo"
                    }
                    """;

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("analise");
        }

        @Test
        @DisplayName("horas acima do maximo configurado sao rejeitadas")
        void horasAcimaDoMaximoSaoRejeitadas() {
            String json = """
                    {
                      "priority": "MEDIUM",
                      "complexity": "LOW",
                      "estimatedHours": 500,
                      "reason": "motivo"
                    }
                    """;

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("(0, 200.0]");
        }

        @Test
        @DisplayName("horas zeradas sao rejeitadas, zero nao e estimativa")
        void horasZeradasSaoRejeitadas() {
            String json = """
                    {
                      "priority": "MEDIUM",
                      "complexity": "LOW",
                      "estimatedHours": 0,
                      "reason": "motivo"
                    }
                    """;

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("estimatedHours");
        }

        @Test
        @DisplayName("horas ausentes sao rejeitadas, nao viram 0.0")
        void horasAusentesSaoRejeitadas() {
            String json = """
                    {
                      "priority": "MEDIUM",
                      "complexity": "LOW",
                      "reason": "motivo"
                    }
                    """;

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("estimatedHours");
        }

        @Test
        @DisplayName("motivo em branco e rejeitado")
        void motivoEmBrancoEhRejeitado() {
            String json = """
                    {
                      "priority": "MEDIUM",
                      "complexity": "LOW",
                      "estimatedHours": 5,
                      "reason": "  "
                    }
                    """;

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("campo 'reason' vazio");
        }

        @Test
        @DisplayName("motivo acima do limite de caracteres e rejeitado")
        void motivoAcimaDoLimiteEhRejeitado() {
            String json = """
                    {
                      "priority": "MEDIUM",
                      "complexity": "LOW",
                      "estimatedHours": 5,
                      "reason": "%s"
                    }
                    """.formatted("r".repeat(5001));

            assertThatThrownBy(() -> validator.validateAnalysis(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("acima do limite de 5000");
        }
    }

    @Nested
    @DisplayName("decomposicao (RF-12)")
    class Decomposicao {

        @Test
        @DisplayName("decomposicao valida devolve as tres subtarefas, com horas opcionais")
        void decomposicaoValidaEhAceita() {
            String json = """
                    {
                      "subtasks": [
                        {"title": "Extrair dados do banco", "description": "Consultar vendas do trimestre", "estimatedHours": 4},
                        {"title": "Montar graficos de receita", "description": "Gerar os graficos do relatorio", "estimatedHours": 3},
                        {"title": "Revisar o texto final", "description": "Checar coesao e numeros do relatorio"}
                      ]
                    }
                    """;

            TaskDecomposition result = validator.validateDecomposition(json);

            assertThat(result.subtasks()).hasSize(3);
            assertThat(result.subtasks().get(0).estimatedHours()).isEqualTo(4);
            assertThat(result.subtasks().get(2).estimatedHours()).isNull();
        }

        @Test
        @DisplayName("uma unica subtarefa fica abaixo do minimo de 2")
        void umaSubtarefaEhRejeitada() {
            String json = """
                    {
                      "subtasks": [
                        {"title": "Unica tarefa", "description": "Descricao da unica tarefa"}
                      ]
                    }
                    """;

            assertThatThrownBy(() -> validator.validateDecomposition(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("quantidade de subtarefas (1)");
        }

        @Test
        @DisplayName("onze subtarefas estouram o maximo de 10")
        void onzeSubtarefasSaoRejeitadas() {
            String subtasks = IntStream.range(0, 11)
                    .mapToObj(i -> "{\"title\": \"tarefa " + i + "\", \"description\": \"descricao " + i + "\"}")
                    .collect(Collectors.joining(", "));
            String json = "{\"subtasks\": [" + subtasks + "]}";

            assertThatThrownBy(() -> validator.validateDecomposition(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("quantidade de subtarefas (11)");
        }

        @Test
        @DisplayName("titulos repetidos com caixa e espacos diferentes sao rejeitados")
        void titulosDuplicadosSaoRejeitados() {
            String json = """
                    {
                      "subtasks": [
                        {"title": "Escrever testes", "description": "Primeira versao"},
                        {"title": "  escrever TESTES ", "description": "Segunda versao"}
                      ]
                    }
                    """;

            assertThatThrownBy(() -> validator.validateDecomposition(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("subtarefa duplicada");
        }

        @Test
        @DisplayName("subtarefa com titulo em branco e rejeitada")
        void subtarefaComTituloVazioEhRejeitada() {
            String json = """
                    {
                      "subtasks": [
                        {"title": "   ", "description": "Descricao valida"},
                        {"title": "Outra tarefa", "description": "Descricao valida"}
                      ]
                    }
                    """;

            assertThatThrownBy(() -> validator.validateDecomposition(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("subtasks[0].title");
        }

        @Test
        @DisplayName("horas de subtarefa fora do intervalo sao rejeitadas")
        void horasDeSubtarefaForaDoIntervaloSaoRejeitadas() {
            String json = """
                    {
                      "subtasks": [
                        {"title": "Tarefa um", "description": "Descricao um", "estimatedHours": 999},
                        {"title": "Tarefa dois", "description": "Descricao dois"}
                      ]
                    }
                    """;

            assertThatThrownBy(() -> validator.validateDecomposition(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("estimatedHours");
        }

        @Test
        @DisplayName("sem o campo subtasks a resposta e rejeitada")
        void listaDeSubtarefasAusenteEhRejeitada() {
            String json = """
                    {"priority": "MEDIUM"}
                    """;

            assertThatThrownBy(() -> validator.validateDecomposition(json))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("subtasks");
        }
    }

    @Nested
    @DisplayName("resposta crua")
    class RespostaCrua {

        @Test
        @DisplayName("texto que nao e JSON e rejeitado")
        void textoNaoJsonEhRejeitado() {
            assertThatThrownBy(() -> validator.validateAnalysis("desculpe, nao posso analisar isso"))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("invalida no caso de analise");
        }

        @Test
        @DisplayName("resposta nula ou em branco e rejeitada")
        void respostaVaziaEhRejeitada() {
            assertThatThrownBy(() -> validator.validateImprovement(null))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("vazia");
            assertThatThrownBy(() -> validator.validateImprovement("   "))
                    .isInstanceOf(InvalidLlmResponseException.class)
                    .hasMessageContaining("vazia");
        }
    }
}
