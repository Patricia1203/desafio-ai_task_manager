package com.desafio.taskmanager.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os três prompts versionados renderizados com as variáveis do contrato
 * (RNF-10). Um .st quebrado — chave de JSON sem escape, variável renomeada,
 * acento corrompido — só estouraria na primeira chamada real ao modelo; aqui
 * ele estoura no build. A leitura é UTF-8 explícita porque os prompts são
 * português com acentos e o texto tem que chegar inteiro ao renderer.
 */
@DisplayName("prompts .st renderizam com as variaveis do contrato")
class PromptsTest {

    private final StTemplateRenderer renderer = StTemplateRenderer.builder().build();

    @Test
    @DisplayName("task-improve.st renderiza titulo, descricao e o JSON escapado")
    void taskImproveRenderiza() throws IOException {
        String out = render("task-improve.st", Map.of(
                "title", "Preparar relatorio",
                "description", "Relatorio trimestral para a diretoria",
                "maxTitleLength", 200,
                "maxTextLength", 5000));

        assertThat(out)
                .contains("Título: Preparar relatorio")
                .contains("{\"title\": \"...\", \"description\": \"...\"}")
                .doesNotContain("{title}")
                .doesNotContain("\\{");
    }

    @Test
    @DisplayName("task-analyze.st fixa os enums permitidos e o JSON escapado")
    void taskAnalyzeRenderiza() throws IOException {
        String out = render("task-analyze.st", Map.of(
                "title", "Preparar relatorio",
                "description", "Relatorio trimestral",
                "priority", "MEDIUM",
                "maxEstimatedHours", 200,
                "maxTextLength", 5000));

        assertThat(out)
                .contains("LOW, MEDIUM, HIGH")
                .contains("LOW, MEDIUM, HIGH")
                .contains("Prioridade atual: MEDIUM")
                .contains("{\"priority\": \"...\", \"complexity\": \"...\"")
                .doesNotContain("\\{");
    }

    @Test
    @DisplayName("task-decompose.st renderiza o intervalo de subtarefas e o JSON de lista")
    void taskDecomposeRenderiza() throws IOException {
        String out = render("task-decompose.st", Map.of(
                "title", "Preparar relatorio",
                "description", "Relatorio trimestral",
                "minSubtasks", 2,
                "maxSubtasks", 10,
                "maxTitleLength", 200,
                "maxTextLength", 5000));

        assertThat(out)
                .contains("entre 2 e 10 no total")
                .contains("{\"subtasks\": [{\"title\": \"...\", \"description\": \"...\", \"estimatedHours\": 4}]}")
                .doesNotContain("\\{");
    }

    @Test
    @DisplayName("variavel sobrando no mapa nao quebra o render, so as do template sao exigidas")
    void variavelExtraNaoQuebraRender() throws IOException {
        String out = render("task-improve.st", Map.of(
                "title", "Titulo",
                "description", "Descricao",
                "maxTitleLength", 200,
                "maxTextLength", 5000,
                "priority", "MEDIUM"));

        assertThat(out).contains("Título: Titulo");
    }

    private String render(String arquivo, Map<String, Object> variables) throws IOException {
        String template = new ClassPathResource("prompts/" + arquivo)
                .getContentAsString(StandardCharsets.UTF_8);
        return renderer.apply(template, variables);
    }
}
