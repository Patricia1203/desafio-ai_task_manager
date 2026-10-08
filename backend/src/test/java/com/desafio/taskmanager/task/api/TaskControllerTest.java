package com.desafio.taskmanager.task.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.common.error.BusinessRuleException;
import com.desafio.taskmanager.common.error.GlobalExceptionHandler;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.application.TaskService;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.application.dto.TaskFilter;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;
import com.desafio.taskmanager.task.domain.TimeUnit;

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
import static org.mockito.ArgumentMatchers.anyBoolean;
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
@Import({GlobalExceptionHandler.class, TaskMapper.class})
class TaskControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService service;

    /**
     * A entidade gera o proprio id, entao os testes leem o id real daqui em vez
     * de fixar uma constante — o service e mockado, mas a coerencia do JSON com
     * a entidade devolvida continua sendo conferida.
     */
    private static Task tarefa() {
        return new Task("Titulo", "Descricao", TaskPriority.HIGH,
                LocalDate.of(2026, 12, 31), null);
    }

    // --- GET /tasks ---

    @Test
    void listarRetorna200ComEnvelopeDePagina() throws Exception {
        Task primeira = tarefa();
        when(service.list(any(TaskFilter.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(primeira), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(primeira.getId().toString()))
                .andExpect(jsonPath("$.content[0].title").value("Titulo"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listarComFiltroDeStatusEPrioridadeRepassaAoService() throws Exception {
        when(service.list(any(TaskFilter.class), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/tasks").param("status", "IN_PROGRESS").param("priority", "HIGH"))
                .andExpect(status().isOk());

        verify(service).list(
                eq(new TaskFilter(TaskStatus.IN_PROGRESS, TaskPriority.HIGH)),
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
                .andExpect(jsonPath("$.pending").value(3))
                .andExpect(jsonPath("$.inProgress").value(2))
                .andExpect(jsonPath("$.done").value(2))
                .andExpect(jsonPath("$.highPriority").value(4));
    }

    // --- GET /tasks/{id} ---

    @Test
    void buscarPorIdRetorna200() throws Exception {
        Task tarefa = tarefa();
        when(service.findById(tarefa.getId())).thenReturn(tarefa);

        mockMvc.perform(get("/tasks/{id}", tarefa.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tarefa.getId().toString()))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-31"))
                .andExpect(jsonPath("$.parentId").doesNotExist());
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
        Task pai = new Task("Pai", null, null, null, null);
        Task sub = new Task("Filha", null, TaskPriority.MEDIUM, null, pai);
        when(service.findSubtasks(pai.getId())).thenReturn(List.of(sub));

        mockMvc.perform(get("/tasks/{id}/subtasks", pai.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Filha"))
                .andExpect(jsonPath("$[0].parentId").value(pai.getId().toString()));
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
        Task criada = tarefa();
        when(service.create(any(TaskCommand.class))).thenReturn(criada);

        mockMvc.perform(post("/api/tasks")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Titulo","description":"Descricao","priority":"HIGH",
                                 "dueDate":"2026-12-31"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tasks/" + criada.getId()))
                .andExpect(jsonPath("$.id").value(criada.getId().toString()))
                .andExpect(jsonPath("$.title").value("Titulo"));
    }

    @Test
    void criarComTempoEstimadoRepassaAoServicoESerializa() throws Exception {
        Task criada = new Task("Instalar", null, null, null, 4.0, TimeUnit.DAYS, null);
        when(service.create(any(TaskCommand.class))).thenReturn(criada);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Instalar","estimatedTime":4.0,"estimatedUnit":"DAYS"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estimatedTime").value(4.0))
                .andExpect(jsonPath("$.estimatedUnit").value("DAYS"));

        ArgumentCaptor<TaskCommand> captor = ArgumentCaptor.forClass(TaskCommand.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().tempoEstimado()).isEqualTo(4.0);
        assertThat(captor.getValue().unidadeTempo()).isEqualTo(TimeUnit.DAYS);
    }

    @Test
    void criarComTempoEstimadoZeradoRetorna400ApontandoOCampo() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","estimatedTime":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("estimatedTime"))
                .andExpect(jsonPath("$.errors[0].reason").value("estimatedTime deve ser maior que zero"));

        verify(service, never()).create(any());
    }

    @Test
    void criarComTempoEstimadoAcimaDoTetoRetorna400() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","estimatedTime":500}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("estimatedTime"));
    }

    @Test
    void criarComAreaIdRepassaAoServicoESerializa() throws Exception {
        Task criada = new Task("Projeto", null, null, null, null);
        com.desafio.taskmanager.area.domain.WorkArea area =
                new com.desafio.taskmanager.area.domain.WorkArea("Pessoal");
        criada.setArea(area);
        when(service.create(any(TaskCommand.class))).thenReturn(criada);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Projeto","areaId":"11111111-1111-1111-1111-111111111111"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.areaId").value(area.getId().toString()));

        ArgumentCaptor<TaskCommand> captor = ArgumentCaptor.forClass(TaskCommand.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().areaId()).isEqualTo(ID);
    }

    @Test
    void listarPorAreaIdRepassaAoFiltro() throws Exception {
        when(service.list(any(TaskFilter.class), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(
                        List.of(tarefa()), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/tasks").param("areaId", ID.toString()))
                .andExpect(status().isOk());

        ArgumentCaptor<TaskFilter> captor = ArgumentCaptor.forClass(TaskFilter.class);
        verify(service).list(captor.capture(), any());
        assertThat(captor.getValue().areaId()).isEqualTo(ID);
    }

    @Test
    void criarSemTituloRetorna400ComListaDeCampos() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"sem titulo"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/validacao"))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo e obrigatorio"));

        verify(service, never()).create(any());
    }

    @Test
    void criarComTituloLongoRetorna400ApontandoOCampo() throws Exception {
        String longo = "a".repeat(201);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + longo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].reason").value("titulo deve ter no maximo 200 caracteres"));
    }

    @Test
    void criarComPrioridadeDesconhecidaRetorna400() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","priority":"URGENTE"}"""))
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
        when(service.update(eq(ID), any(TaskCommand.class))).thenReturn(tarefa());

        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Editado","description":"d","priority":"LOW"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titulo"));
    }

    @Test
    void editarSemTituloRetorna400() throws Exception {
        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"sem titulo"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(service, never()).update(any(), any());
    }

    @Test
    void editarInexistenteRetorna404() throws Exception {
        when(service.update(eq(ID), any(TaskCommand.class)))
                .thenThrow(ResourceNotFoundException.of("tarefa", ID));

        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"X"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void editarIgnoraStatusEnviadoNoCorpo() throws Exception {
        when(service.update(eq(ID), any(TaskCommand.class))).thenReturn(tarefa());

        mockMvc.perform(put("/tasks/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"X","status":"DONE"}"""))
                .andExpect(status().isOk());

        // status nao existe em TaskCommand: se aparecesse no log de called com
        // status, o mapeamento teria mudado de tipo
        verify(service).update(eq(ID), any(TaskCommand.class));
    }

    // --- PATCH /tasks/{id}/status ---

    @Test
    void alterarStatusRetorna200() throws Exception {
        when(service.changeStatus(ID, TaskStatus.IN_PROGRESS, false)).thenReturn(tarefa());

        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}"""))
                .andExpect(status().isOk());

        verify(service).changeStatus(ID, TaskStatus.IN_PROGRESS, false);
    }

    @Test
    void concluirComFlagRepassaOCompleteSubtasks() throws Exception {
        when(service.changeStatus(ID, TaskStatus.DONE, true)).thenReturn(tarefa());

        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE","completeSubtasks":true}"""))
                .andExpect(status().isOk());

        verify(service).changeStatus(ID, TaskStatus.DONE, true);
    }

    @Test
    void alterarStatusSemCampoRetorna400() throws Exception {
        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verify(service, never()).changeStatus(any(), any(TaskStatus.class), anyBoolean());
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
        when(service.changeStatus(ID, TaskStatus.IN_PROGRESS, false))
                .thenThrow(new BusinessRuleException("status nao pode ir de DONE para IN_PROGRESS"));

        mockMvc.perform(patch("/tasks/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://desafio.ai-task-manager/errors/regra-de-negocio"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("DONE")));
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