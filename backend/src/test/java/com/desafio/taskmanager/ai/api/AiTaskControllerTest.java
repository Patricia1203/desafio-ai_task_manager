package com.desafio.taskmanager.ai.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.desafio.taskmanager.ai.port.dto.ProposedSubtask;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskComplexity;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.common.error.GlobalExceptionHandler;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.common.web.TraceIdFilter;
import com.desafio.taskmanager.task.api.TaskMapper;
import com.desafio.taskmanager.task.application.AiTaskService;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP da API de IA com o service mockado: rotas, status
 * (200/201/400/404/502), shape em portugues do RF-24 e formato ProblemDetail.
 * As regras de negocio ficam no {@code AiTaskServiceTest}.
 */
@WebMvcTest(AiTaskController.class)
@Import({GlobalExceptionHandler.class, TaskMapper.class, TraceIdFilter.class})
class AiTaskControllerTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiTaskService service;

    private static Task tarefa() {
        return new Task("Titulo", "Descricao", TaskPriority.MEDIUM, null, null);
    }

    // --- POST /ai/tasks/improve ---

    @Test
    void improveRetorna200ComOsCamposEmIngles() throws Exception {
        when(service.improve("Titulo", "Descricao"))
                .thenReturn(new TaskImprovement("Novo titulo", "Nova descricao"));

        mockMvc.perform(post("/ai/tasks/improve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Titulo","description":"Descricao"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Novo titulo"))
                .andExpect(jsonPath("$.description").value("Nova descricao"));
    }

    @Test
    void improveSemTituloRetorna400ComOCampo() throws Exception {
        mockMvc.perform(post("/ai/tasks/improve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"so descricao"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo e obrigatorio"));

        verifyNoInteractions(service);
    }

    // --- POST /ai/tasks/{id}/analyze ---

    @Test
    void analyzeRetorna200ComAAnaliseTraduzida() throws Exception {
        when(service.analyze(ID)).thenReturn(
                new TaskAnalysis(TaskPriority.HIGH, TaskComplexity.MEDIUM, 12.5, "justificativa"));

        mockMvc.perform(post("/ai/tasks/{id}/analyze", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.complexity").value("MEDIUM"))
                .andExpect(jsonPath("$.estimatedHours").value(12.5))
                .andExpect(jsonPath("$.reason").value("justificativa"));
    }

    @Test
    void analyzeComIdInexistenteRetorna404() throws Exception {
        when(service.analyze(ID)).thenThrow(ResourceNotFoundException.of("tarefa", ID));

        mockMvc.perform(post("/ai/tasks/{id}/analyze", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", containsString("nao-encontrado")))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    // --- POST /ai/tasks/{id}/decompose ---

    @Test
    void decomposeRetorna200ComAsSugestoesEmIngles() throws Exception {
        when(service.decompose(ID)).thenReturn(new TaskDecomposition(List.of(
                new ProposedSubtask("Subtitulo 1", "Subdescricao 1", 2.0),
                new ProposedSubtask("Subtitulo 2", "Subdescricao 2", null))));

        mockMvc.perform(post("/ai/tasks/{id}/decompose", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtasks.length()").value(2))
                .andExpect(jsonPath("$.subtasks[0].title").value("Subtitulo 1"))
                .andExpect(jsonPath("$.subtasks[0].estimatedHours").value(2.0))
                .andExpect(jsonPath("$.subtasks[1].title").value("Subtitulo 2"));
    }

    // --- POST /ai/tasks/{id}/decompose/apply ---

    @Test
    void applyRetorna201ComAsCriadasEOLocationParaAsSubtarefas() throws Exception {
        Task pai = tarefa();
        when(service.apply(eq(ID), any())).thenReturn(List.of(
                new Task("Sub 1", "Descricao 1", TaskPriority.LOW, null, pai),
                new Task("Sub 2", null, null, null, pai)));
        String corpo = """
                {"subtasks":[
                    {"title":"Sub 1","description":"Descricao 1","estimatedHours":2.0},
                    {"title":"Sub 2","description":null,"estimatedHours":null}
                ]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/tasks/" + ID + "/subtasks")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Sub 1"))
                .andExpect(jsonPath("$[0].parentId").value(pai.getId().toString()))
                .andExpect(jsonPath("$[1].title").value("Sub 2"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).apply(eq(ID), captor.capture());
        assertThat(captor.getValue()).extracting(TaskCommand::titulo)
                .containsExactly("Sub 1", "Sub 2");
        assertThat(captor.getValue().get(1).prioridade()).isNull();
        assertThat(captor.getValue().get(0).tempoEstimado()).isEqualTo(2.0);
        assertThat(captor.getValue().get(0).unidadeTempo()).isEqualTo(TimeUnit.HOURS);
        assertThat(captor.getValue().get(1).tempoEstimado()).isNull();
    }

    @Test
    void applyComTituloEmBrancoRetorna400ApontandoOItemDaLista() throws Exception {
        String corpo = """
                {"subtasks":[{"title":"","description":"x","estimatedHours":1.0}]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtasks[0].title"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo nao pode ser vazio"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComHorasForaDoIntervaloRetorna400() throws Exception {
        String corpo = """
                {"subtasks":[{"title":"Sub","description":null,"estimatedHours":500.0}]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtasks[0].estimatedHours"))
                .andExpect(jsonPath("$.errors[0].reason").value("horasEstimadas deve ter no maximo 200"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComListaVaziaRetorna400() throws Exception {
        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subtasks":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtasks"))
                .andExpect(jsonPath("$.errors[0].reason").value("subtarefas nao pode ser vazia"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComMaisDezSubtarefasRetorna400() throws Exception {
        String itens = IntStream.range(0, 11)
                .mapToObj(i -> "{\"title\":\"Sub " + i + "\",\"description\":null,\"estimatedHours\":null}")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subtasks\":[" + itens + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtasks"))
                .andExpect(jsonPath("$.errors[0].reason").value("subtarefas deve ter no maximo 10 itens"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComIdInexistenteRetorna404() throws Exception {
        when(service.apply(eq(ID), any())).thenThrow(ResourceNotFoundException.of("tarefa", ID));
        String corpo = """
                {"subtasks":[{"title":"Sub","description":null,"estimatedHours":null}]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", containsString("nao-encontrado")));
    }

    // --- erro de LLM ---

    @Test
    void respostaInvalidaDaIaRetorna502ComOCodigoLlmInvalidResponse() throws Exception {
        when(service.analyze(ID)).thenThrow(
                new InvalidLlmResponseException("resposta da IA invalida apos 2 tentativas: motivo"));

        mockMvc.perform(post("/ai/tasks/{id}/analyze", ID))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", containsString("resposta-llm-invalida")))
                .andExpect(jsonPath("$.title").value("Resposta invalida da IA"))
                .andExpect(jsonPath("$.code").value("LLM_INVALID_RESPONSE"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                // erro de IA herda o envelope de erro comum (T-F02-05k)
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }
}
