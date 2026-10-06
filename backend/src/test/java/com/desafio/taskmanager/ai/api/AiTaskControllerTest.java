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
import com.desafio.taskmanager.task.api.TaskMapper;
import com.desafio.taskmanager.task.application.AiTaskService;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;

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
@Import({GlobalExceptionHandler.class, TaskMapper.class})
class AiTaskControllerTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiTaskService service;

    private static Task tarefa() {
        return new Task("Titulo", "Descricao", TaskPriority.MEDIA, null, null);
    }

    // --- POST /ai/tasks/improve ---

    @Test
    void improveRetorna200ComOsCamposEmPortugues() throws Exception {
        when(service.improve("Titulo", "Descricao"))
                .thenReturn(new TaskImprovement("Novo titulo", "Nova descricao"));

        mockMvc.perform(post("/ai/tasks/improve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Titulo","descricao":"Descricao"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Novo titulo"))
                .andExpect(jsonPath("$.descricao").value("Nova descricao"));
    }

    @Test
    void improveSemTituloRetorna400ComOCampo() throws Exception {
        mockMvc.perform(post("/ai/tasks/improve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"so descricao"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("titulo"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo e obrigatorio"));

        verifyNoInteractions(service);
    }

    // --- POST /ai/tasks/{id}/analyze ---

    @Test
    void analyzeRetorna200ComAAnaliseTraduzida() throws Exception {
        when(service.analyze(ID)).thenReturn(
                new TaskAnalysis(TaskPriority.ALTA, TaskComplexity.MEDIUM, 12.5, "justificativa"));

        mockMvc.perform(post("/ai/tasks/{id}/analyze", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.complexidade").value("MEDIUM"))
                .andExpect(jsonPath("$.horasEstimadas").value(12.5))
                .andExpect(jsonPath("$.justificativa").value("justificativa"));
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
    void decomposeRetorna200ComAsSugestoesEmPortugues() throws Exception {
        when(service.decompose(ID)).thenReturn(new TaskDecomposition(List.of(
                new ProposedSubtask("Subtitulo 1", "Subdescricao 1", 2.0),
                new ProposedSubtask("Subtitulo 2", "Subdescricao 2", null))));

        mockMvc.perform(post("/ai/tasks/{id}/decompose", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtarefas.length()").value(2))
                .andExpect(jsonPath("$.subtarefas[0].titulo").value("Subtitulo 1"))
                .andExpect(jsonPath("$.subtarefas[0].horasEstimadas").value(2.0))
                .andExpect(jsonPath("$.subtarefas[1].titulo").value("Subtitulo 2"));
    }

    // --- POST /ai/tasks/{id}/decompose/apply ---

    @Test
    void applyRetorna201ComAsCriadasEOLocationParaAsSubtarefas() throws Exception {
        Task pai = tarefa();
        when(service.apply(eq(ID), any())).thenReturn(List.of(
                new Task("Sub 1", "Descricao 1", TaskPriority.BAIXA, null, pai),
                new Task("Sub 2", null, null, null, pai)));
        String corpo = """
                {"subtarefas":[
                    {"titulo":"Sub 1","descricao":"Descricao 1","horasEstimadas":2.0},
                    {"titulo":"Sub 2","descricao":null,"horasEstimadas":null}
                ]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/tasks/" + ID + "/subtasks")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titulo").value("Sub 1"))
                .andExpect(jsonPath("$[0].idTarefaPai").value(pai.getId().toString()))
                .andExpect(jsonPath("$[1].titulo").value("Sub 2"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).apply(eq(ID), captor.capture());
        assertThat(captor.getValue()).extracting(TaskCommand::titulo)
                .containsExactly("Sub 1", "Sub 2");
        assertThat(captor.getValue().get(1).prioridade()).isNull();
    }

    @Test
    void applyComTituloEmBrancoRetorna400ApontandoOItemDaLista() throws Exception {
        String corpo = """
                {"subtarefas":[{"titulo":"","descricao":"x","horasEstimadas":1.0}]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtarefas[0].titulo"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo nao pode ser vazio"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComHorasForaDoIntervaloRetorna400() throws Exception {
        String corpo = """
                {"subtarefas":[{"titulo":"Sub","descricao":null,"horasEstimadas":500.0}]}
                """;

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtarefas[0].horasEstimadas"))
                .andExpect(jsonPath("$.errors[0].reason").value("horasEstimadas deve ter no maximo 200"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComListaVaziaRetorna400() throws Exception {
        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subtarefas":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtarefas"))
                .andExpect(jsonPath("$.errors[0].reason").value("subtarefas nao pode ser vazia"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComMaisDezSubtarefasRetorna400() throws Exception {
        String itens = IntStream.range(0, 11)
                .mapToObj(i -> "{\"titulo\":\"Sub " + i + "\",\"descricao\":null,\"horasEstimadas\":null}")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/ai/tasks/{id}/decompose/apply", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subtarefas\":[" + itens + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("subtarefas"))
                .andExpect(jsonPath("$.errors[0].reason").value("subtarefas deve ter no maximo 10 itens"));

        verifyNoInteractions(service);
    }

    @Test
    void applyComIdInexistenteRetorna404() throws Exception {
        when(service.apply(eq(ID), any())).thenThrow(ResourceNotFoundException.of("tarefa", ID));
        String corpo = """
                {"subtarefas":[{"titulo":"Sub","descricao":null,"horasEstimadas":null}]}
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
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
