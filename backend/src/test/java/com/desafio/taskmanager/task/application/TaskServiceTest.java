package com.desafio.taskmanager.task.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.common.error.BusinessRuleException;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.api.dto.CreateTaskRequest;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.api.dto.UpdateTaskRequest;
import com.desafio.taskmanager.task.application.dto.TaskFilter;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;
import com.desafio.taskmanager.task.infra.TaskRepository;

import com.desafio.taskmanager.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TST-04: as regras de negocio contra Postgres real. Nao e mock: o service
 * monta Specification, o repositório gera JPQL e o banco valida — o que o
 * teste precisa provar e que a regra vale no dado persistido.
 *
 * <p>O container vem de {@link PostgresIntegrationTest}: o banco é compartilhado
 * com as outras classes de integração e o {@code @BeforeEach} limpa a tabela, já
 * que o container é o mesmo.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class TaskServiceTest extends PostgresIntegrationTest {

    @Autowired
    private TaskService service;

    @Autowired
    private TaskRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    // --- criar (RF-01) ---

    @Test
    void criarNasceComStatusInicialEPrioridadePadrao() {
        TaskResponse criada = service.create(pedido("Nova tarefa", null, null, null));

        assertThat(criada.status()).isEqualTo(TaskStatus.A_FAZER);
        assertThat(criada.prioridade()).isEqualTo(TaskPriority.MEDIA);
        assertThat(criada.id()).isNotNull();
        assertThat(criada.criadoEm()).isNotNull();
        assertThat(criada.atualizadoEm()).isNotNull();
    }

    @Test
    void criarComPrioridadeEPrazoGravaOsCampos() {
        TaskResponse criada = service.create(
                pedido("Com prazo", "descricao", TaskPriority.ALTA, LocalDate.of(2026, 12, 31)));

        assertThat(criada.prioridade()).isEqualTo(TaskPriority.ALTA);
        assertThat(criada.prazo()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(criada.descricao()).isEqualTo("descricao");
    }

    @Test
    void criarComTituloEmBrancoEhRecusado() {
        assertThatThrownBy(() -> service.create(pedido("   ", null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("titulo");
    }

    @Test
    void criarComTituloLongoEhRecusado() {
        assertThatThrownBy(() -> service.create(pedido("a".repeat(201), null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("200");
    }

    // --- ler (RF-02) ---

    @Test
    void buscarPorIdDevolveATarefa() {
        UUID id = criar("Busca").id();

        TaskResponse achada = service.findById(id);

        assertThat(achada.id()).isEqualTo(id);
        assertThat(achada.titulo()).isEqualTo("Busca");
    }

    @Test
    void buscarPorIdInexistenteRetorna404() {
        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    void listarSemFiltroDevolveTodas() {
        criar("A");
        criar("B");

        assertThat(service.list(TaskFilter.all(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(service.findAll(TaskFilter.all())).hasSize(2);
    }

    @Test
    void listarComFiltroNullNaoFiltraNada() {
        criar("A");

        assertThat(service.list(null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(service.findAll(null)).hasSize(1);
    }

    @Test
    void filtrarPorStatusRetornaSomenteODesejado() {
        service.changeStatus(criar("Concluida").id(), TaskStatus.CONCLUIDA);
        criar("Pendente");

        assertThat(service.findAll(TaskFilter.byStatus(TaskStatus.A_FAZER)))
                .extracting(TaskResponse::titulo)
                .containsExactly("Pendente");
        assertThat(service.findAll(TaskFilter.byStatus(TaskStatus.CONCLUIDA)))
                .extracting(TaskResponse::titulo)
                .containsExactly("Concluida");
    }

    @Test
    void filtrarPorPrioridadeRetornaSomenteODesejada() {
        criar("Alta", null, TaskPriority.ALTA, null);
        criar("Baixa", null, TaskPriority.BAIXA, null);

        assertThat(service.findAll(TaskFilter.byPriority(TaskPriority.ALTA)))
                .extracting(TaskResponse::titulo)
                .containsExactly("Alta");
    }

    @Test
    void filtrosCompostosSeIntersecao() {
        service.changeStatus(criar("Alta pendente", null, TaskPriority.ALTA, null).id(), TaskStatus.EM_ANDAMENTO);
        criar("Alta concluida", null, TaskPriority.ALTA, null);
        criar("Media pendente", null, TaskPriority.MEDIA, null);

        List<TaskResponse> resultado = service.findAll(
                new TaskFilter(TaskStatus.EM_ANDAMENTO, TaskPriority.ALTA));

        assertThat(resultado).extracting(TaskResponse::titulo).containsExactly("Alta pendente");
    }

    @Test
    void listarPaginadoRespeitaSizeEOrdenacao() {
        criar("A");
        criar("B");
        criar("C");

        Page<TaskResponse> primeira = service.list(
                TaskFilter.all(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title")));

        assertThat(primeira.getTotalElements()).isEqualTo(3);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.getContent()).extracting(TaskResponse::titulo).containsExactly("A", "B");
    }

    // --- editar (RF-03) ---

    @Test
    void editarAtualizaConteudoEMantemStatus() {
        TaskResponse original = criar("Antigo", "antes", TaskPriority.BAIXA, null);
        service.changeStatus(original.id(), TaskStatus.EM_ANDAMENTO);

        TaskResponse editada = service.update(original.id(),
                new UpdateTaskRequest("  Novo  ", "depois", TaskPriority.ALTA, LocalDate.of(2026, 6, 1)));

        assertThat(editada.titulo()).isEqualTo("Novo");
        assertThat(editada.descricao()).isEqualTo("depois");
        assertThat(editada.prioridade()).isEqualTo(TaskPriority.ALTA);
        assertThat(editada.prazo()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(editada.status()).isEqualTo(TaskStatus.EM_ANDAMENTO);
    }

    @Test
    void editarTarefaInexistenteRetorna404() {
        assertThatThrownBy(() -> service.update(UUID.randomUUID(),
                new UpdateTaskRequest("X", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editarComTituloEmBrancoEhRecusado() {
        UUID id = criar("Original").id();

        assertThatThrownBy(() -> service.update(id, new UpdateTaskRequest("  ", null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- status (RF-06) ---

    @Test
    void alterarStatusPersisteNoBanco() {
        UUID id = criar("Fluxo").id();

        assertThat(service.changeStatus(id, TaskStatus.EM_ANDAMENTO).status()).isEqualTo(TaskStatus.EM_ANDAMENTO);
        assertThat(service.findById(id).status()).isEqualTo(TaskStatus.EM_ANDAMENTO);
        assertThat(service.changeStatus(id, TaskStatus.CONCLUIDA).status()).isEqualTo(TaskStatus.CONCLUIDA);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(TaskStatus.CONCLUIDA);
    }

    @Test
    void alterarParaOMesmoStatusNaoFalhaNemMexeEmUpdatedAt() {
        UUID id = criar("Estavel").id();
        // relê para comparar com o valor do banco: timestamptz guarda microssegundos
        Instant persistido = service.findById(id).atualizadoEm();

        TaskResponse igual = service.changeStatus(id, TaskStatus.A_FAZER);

        assertThat(igual.status()).isEqualTo(TaskStatus.A_FAZER);
        assertThat(service.findById(id).atualizadoEm()).isEqualTo(persistido);
    }

    @Test
    void voltarDeConcluidaParaEmAndamentoEhRecusado() {
        UUID id = criar("Concluida").id();
        service.changeStatus(id, TaskStatus.CONCLUIDA);

        assertThatThrownBy(() -> service.changeStatus(id, TaskStatus.EM_ANDAMENTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CONCLUIDA");
    }

    @Test
    void reabrirDeConcluidaParaAFazerEhPermitido() {
        UUID id = criar("Concluida").id();
        service.changeStatus(id, TaskStatus.CONCLUIDA);

        assertThat(service.changeStatus(id, TaskStatus.A_FAZER).status()).isEqualTo(TaskStatus.A_FAZER);
    }

    @Test
    void alterarStatusDeTarefaInexistenteRetorna404() {
        assertThatThrownBy(() -> service.changeStatus(UUID.randomUUID(), TaskStatus.CONCLUIDA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- excluir (RF-05) ---

    @Test
    void excluirRemoveATarefa() {
        UUID id = criar("Some").id();

        service.delete(id);

        assertThat(repository.findById(id)).isEmpty();
        assertThat(service.list(TaskFilter.all(), PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    @Test
    void excluirInexistenteRetorna404() {
        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void excluirPaiRemoveSubtarefasPorCascata() {
        TaskResponse pai = criar("Pai");
        criarSubtask(pai.id(), "Filha 1");
        criarSubtask(pai.id(), "Filha 2");

        service.delete(pai.id());

        assertThat(service.findAll(TaskFilter.all())).isEmpty();
    }

    // --- subtarefas ---

    @Test
    void criarSubtaskLigaAoPai() {
        TaskResponse pai = criar("Pai");
        TaskResponse filha = criarSubtask(pai.id(), "Filha");

        assertThat(filha.idTarefaPai()).isEqualTo(pai.id());
        assertThat(service.findSubtasks(pai.id()))
                .extracting(TaskResponse::titulo)
                .containsExactly("Filha");
    }

    @Test
    void criarSubtaskDePaiInexistenteRetorna404() {
        assertThatThrownBy(() -> service.createSubtask(UUID.randomUUID(), pedido("Orfa", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarSubtarefasDePaiInexistenteRetorna404() {
        assertThatThrownBy(() -> service.findSubtasks(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarSubtarefasDeTarefaSemFilhasDevolveVazio() {
        assertThat(service.findSubtasks(criar("Solitaria").id())).isEmpty();
    }

    // --- summary (RF-20) ---

    @Test
    void summaryComBancoVazioTemTudoZero() {
        assertThat(service.summary())
                .isEqualTo(new TaskSummary(0, 0, 0, 0, 0));
    }

    @Test
    void summaryContaCadaIndicador() {
        service.changeStatus(criar("P1").id(), TaskStatus.CONCLUIDA);
        service.changeStatus(criar("P2").id(), TaskStatus.CONCLUIDA);
        service.changeStatus(criar("Em1").id(), TaskStatus.EM_ANDAMENTO);
        criar("Alta1", null, TaskPriority.ALTA, null);
        criar("Alta2", null, TaskPriority.ALTA, null);
        criar("P3");

        // pendentes = Alta1 + Alta2 + P3 (as duas ALTA continuam A_FAZER)
        TaskSummary summary = service.summary();

        assertThat(summary.total()).isEqualTo(6);
        assertThat(summary.pendentes()).isEqualTo(3);
        assertThat(summary.emAndamento()).isEqualTo(1);
        assertThat(summary.concluidas()).isEqualTo(2);
        assertThat(summary.altaPrioridade()).isEqualTo(2);
    }

    @Test
    void summarySomaPorStatusIgualAoTotal() {
        service.changeStatus(criar("A").id(), TaskStatus.CONCLUIDA);
        service.changeStatus(criar("B").id(), TaskStatus.EM_ANDAMENTO);
        criar("C");
        criar("D", null, TaskPriority.ALTA, null);

        TaskSummary summary = service.summary();

        assertThat(summary.pendentes() + summary.emAndamento() + summary.concluidas())
                .isEqualTo(summary.total());
        assertThat(summary.altaPrioridade()).isLessThanOrEqualTo(summary.total());
    }

    // --- helpers ---

    private static CreateTaskRequest pedido(String title, String description, TaskPriority priority,
            LocalDate dueDate) {
        return new CreateTaskRequest(title, description, priority, dueDate);
    }

    private TaskResponse criar(String title) {
        return criar(title, null, null, null);
    }

    private TaskResponse criar(String title, String description, TaskPriority priority, LocalDate dueDate) {
        return service.create(pedido(title, description, priority, dueDate));
    }

    private TaskResponse criarSubtask(UUID parentId, String title) {
        return service.createSubtask(parentId, pedido(title, null, null, null));
    }
}