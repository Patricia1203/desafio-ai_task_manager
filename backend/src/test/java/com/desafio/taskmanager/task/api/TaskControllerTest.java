package com.desafio.taskmanager.task.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.common.error.BusinessRuleException;
import com.desafio.taskmanager.common.error.GlobalExceptionHandler;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.api.dto.CreateTaskRequest;
import com.desafio.taskmanager.task.api.dto.PageResponse;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.api.dto.UpdateTaskRequest;
import com.desafio.taskmanager.task.application.TaskService;
import com.desafio.taskmanager.task.application.dto.TaskFilter;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TST-02: contrato HTTP de tarefas com o service mockado. O que importa aqui e
 * rota, status code, shape do JSON e o formato ProblemDetail — as regras de
 * negocio sao testadas contra o banco em {@code TaskServiceTest}.
 */
@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService service;

    private static TaskResponse resposta() {
        return new TaskResponse(
                ID, "Titulo", "Descricao", TaskStatus.A_FAZER, TaskPriority.ALTA,
                LocalDate.of(2026, 12, 31), null,
                Instant.parse("2026-10-05T10:00:00Z"), Instant.parse("2026-10-05T10:00:00Z"));
    }

    // --- GET /tasks ---

    @Test
    void listarRetorna200ComEnvelopeDePagina() throws Exception {
        when(service.list(any(TaskFilter.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(resposta()), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Titulo"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primeira").value(true))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void listarComFiltroDeStatusEPrioridadeRepassaAoService() throws Exception {
        when(service.list(any(TaskFilter.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/tasks").param("status", "EM_ANDAMENTO").param("priority", "ALTA"))
                .andExpect(status().isOk());

        verify(service).list(
                eq(new TaskFilter(TaskStatus.EM_ANDAMENTO, TaskPriority.ALTA)),
                any());
    }

    /**
     * RNF-03: a ordenacao precisa ser totalmente determinada, senao a mesma
     * tarefa aparece em duas paginas ou some de uma. Com createdAt sozinho o
     * empate de timestamp deixa o Postgres livre para mudar a ordem entre
     * consultas; o id como desempate remove essa liberdade.
     */
    @Test
    void listarUsaCreatedAtEIdComoDesempate() throws Exception {
        when(service.list(any(TaskFilter.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/tasks")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(any(TaskFilter.class), captor.capture());

        Sort.Order porData = captor.getValue().getSort().getOrderFor("createdAt");
        assertThat(porData).isNotNull();
        assertThat(porData.getDirection()).isEqualTo(Sort.Direction.DESC);

        Sort.Order porId = captor.getValue().getSort().getOrderFor("id");
        assertThat(porId).isNotNull();
        assertThat(porId.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    /** O desempate so tem valor se for a ultima clausula do sort. */
    @Test
    void idEhUltimoCriterioDaOrdenacao() throws Exception {
        when(service.list(any(TaskFilter.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/tasks")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(any(TaskFilter.class), captor.capture());

        assertThat(captor.getValue().getSort())
                .containsExactly(
                        new Sort.Order(Sort.Direction.DESC, "createdAt"),
                        new Sort.Order(Sort.Direction.ASC, "id"));
    }

    @Test
    void listarComStatusDesconhecidoRetorna400() throws Exception {
        mockMvc.perform(get("/tasks").param("status", "FINALIZADA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void listarComPageNegativaRetorna400ComCampo() throws Exception {
        mockMvc.perform(get("/tasks").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void listarComSizeAcimaDoLimiteRetorna400() throws Exception {
        mockMvc.perform(get("/tasks").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    // --- GET /tasks/summary ---

    @Test
    void summaryRetorna200ComOsCincoIndicadores() throws Exception {
        when(service.summary()).thenReturn(new TaskSummary(7, 3, 2, 2, 4));

        mockMvc.perform(get("/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.pendentes").value(3))
                .andExpect(jsonPath("$.emAndamento").value(2))
                .andExpect(jsonPath("$.concluidas").value(2))
                .andExpect(jsonPath("$.altaPrioridade").value(4));
    }

    // --- GET /tasks/{id} ---

    @Test
    void buscarPorIdRetorna200() throws Exception {
        when(service.findById(ID)).thenReturn(resposta());

        mockMvc.perform(get("/tasks/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("A_FAZER"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.prazo").value("2026-12-31"))
                .andExpect(jsonPath("$.idTarefaPai").doesNotExist());
    }

    @Test
    void buscarPorIdInexistenteRetorna404EmProblemDetail() throws Exception {
        when(service.findById(ID)).thenThrow(ResourceNotFoundException.of("tarefa", ID));

        mockMvc.perform(get("/tasks/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/nao-encontrado"))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(ID.toString())));
    }

    @Test
    void buscarPorIdQueNaoEhUuidRetorna400() throws Exception {
        mockMvc.perform(get("/tasks/{id}", "nao-e-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/requisicao-malformada"));
    }

    // --- GET /tasks/{id}/subtasks ---

    @Test
    void listarSubtarefasRetorna200() throws Exception {
        TaskResponse sub = new TaskResponse(UUID.randomUUID(), "Filha", null, TaskStatus.A_FAZER,
                TaskPriority.MEDIA, null, ID, Instant.now(), Instant.now());
        when(service.findSubtasks(ID)).thenReturn(List.of(sub));

        mockMvc.perform(get("/tasks/{id}/subtasks", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Filha"))
                .andExpect(jsonPath("$[0].idTarefaPai").value(ID.toString()));
    }

    @Test
    void listarSubtarefasDePaiInexistenteRetorna404() throws Exception {
        when(service.findSubtasks(ID)).thenThrow(ResourceNotFoundException.of("tarefa", ID));

        mockMvc.perform(get("/tasks/{id}/subtasks", ID))
                .andExpect(status().isNotFound());
    }

    // --- POST /tasks ---

    /**
     * O {@code context-path} e /api, entao o Location precisa do prefixo: um
     * Location sem ele aponta para um caminho que nao existe e o cliente toma
     * 404 ao seguir o proprio header do 201. O MockMvc nao aplica o
     * context-path sozinho, entao a requisicao o declara explicitamente.
     */
    @Test
    void criarRetorna201ComLocationIncluindoOContextPath() throws Exception {
        when(service.create(any(CreateTaskRequest.class))).thenReturn(resposta());

        mockMvc.perform(post("/api/tasks")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Titulo","descricao":"Descricao","prioridade":"ALTA",
                                 "prazo":"2026-12-31"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tasks/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.titulo").value("Titulo"));
    }

    @Test
    void criarSemTituloRetorna400ComListaDeCampos() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"sem titulo"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/validacao"))
                .andExpect(jsonPath("$.errors[0].field").value("titulo"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo e obrigatorio"));

        verify(service, never()).create(any());
    }

    @Test
    void criarComTituloLongoRetorna400ApontandoOCampo() throws Exception {
        String longo = "a".repeat(201);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + longo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("titulo"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo deve ter no maximo 200 caracteres"));
    }

    @Test
    void criarComPrioridadeDesconhecidaRetorna400() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"T","prioridade":"URGENTE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/requisicao-malformada"));
    }

    @Test
    void criarComJsonMalformadoRetorna400SemDetalheInterno() throws Exception {
        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Corpo ou parametro de requisicao invalido"))
                .andReturn();

        String corpo = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(corpo)
                .doesNotContain("Exception")
                .doesNotContain("com.fasterxml")
                .doesNotContain("line:");
    }

    // --- PUT /tasks/{id} ---

    @Test
    void editarRetorna200() throws Exception {
        when(service.update(eq(ID), any(UpdateTaskRequest.class))).thenReturn(resposta());

        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Editado","descricao":"d","prioridade":"BAIXA"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Titulo"));
    }

    @Test
    void editarSemTituloRetorna400() throws Exception {
        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"sem titulo"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("titulo"));

        verify(service, never()).update(any(), any());
    }

    @Test
    void editarInexistenteRetorna404() throws Exception {
        when(service.update(eq(ID), any(UpdateTaskRequest.class)))
                .thenThrow(ResourceNotFoundException.of("tarefa", ID));

        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"X"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void editarIgnoraStatusEnviadoNoCorpo() throws Exception {
        when(service.update(eq(ID), any(UpdateTaskRequest.class))).thenReturn(resposta());

        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"X","status":"CONCLUIDA"}"""))
                .andExpect(status().isOk());

        // status nao existe em UpdateTaskRequest: se aparecesse no log de called
        // com status, o mapper teria mudado de tipo
        verify(service).update(eq(ID), any(UpdateTaskRequest.class));
    }

    // --- PATCH /tasks/{id}/status ---

    @Test
    void alterarStatusRetorna200() throws Exception {
        when(service.changeStatus(ID, TaskStatus.EM_ANDAMENTO)).thenReturn(resposta());

        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"EM_ANDAMENTO"}"""))
                .andExpect(status().isOk());

        verify(service).changeStatus(ID, TaskStatus.EM_ANDAMENTO);
    }

    @Test
    void alterarStatusSemCampoRetorna400() throws Exception {
        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verify(service, never()).changeStatus(any(), any());
    }

    @Test
    void alterarStatusComValorDesconhecidoRetorna400() throws Exception {
        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transicaoRecusadaRetorna422() throws Exception {
        when(service.changeStatus(ID, TaskStatus.EM_ANDAMENTO))
                .thenThrow(new BusinessRuleException("status nao pode ir de CONCLUIDA para EM_ANDAMENTO"));

        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"EM_ANDAMENTO"}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/regra-de-negocio"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("CONCLUIDA")));
    }

    // --- DELETE /tasks/{id} ---

    @Test
    void excluirRetorna204SemCorpo() throws Exception {
        mockMvc.perform(delete("/tasks/{id}", ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(ID);
    }

    @Test
    void excluirInexistenteRetorna404() throws Exception {
        org.mockito.Mockito.doThrow(ResourceNotFoundException.of("tarefa", ID)).when(service).delete(ID);

        mockMvc.perform(delete("/tasks/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/nao-encontrado"));
    }

    @Test
    void erroInesperadoRetorna500ComMensagemFixaESemStackTrace() throws Exception {
        when(service.findById(ID)).thenThrow(new IllegalStateException("conexao perdida em /srv/app/x.java"));

        MvcResult result = mockMvc.perform(get("/tasks/{id}", ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Erro interno. Tente novamente mais tarde."))
                .andReturn();

        String corpo = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(corpo)
                .doesNotContain("IllegalStateException")
                .doesNotContain("conexao perdida")
                .doesNotContain("/srv/app/x.java")
                .doesNotContain("\\tat ");
    }
}