package com.desafio.taskmanager.task.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.area.infra.WorkAreaRepository;
import com.desafio.taskmanager.common.error.BusinessRuleException;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.application.dto.TaskFilter;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;
import com.desafio.taskmanager.task.domain.TimeUnit;
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

    @Autowired
    private WorkAreaRepository areas;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        areas.deleteAll();
    }

    // --- criar (RF-01) ---

    @Test
    void criarNasceComStatusInicialEPrioridadePadrao() {
        Task criada = service.create(pedido("Nova tarefa", null, null, null));

        assertThat(criada.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(criada.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(criada.getId()).isNotNull();
        assertThat(criada.getCreatedAt()).isNotNull();
        assertThat(criada.getUpdatedAt()).isNotNull();
    }

    @Test
    void criarComPrioridadeEPrazoGravaOsCampos() {
        Task criada = service.create(
                pedido("Com prazo", "descricao", TaskPriority.HIGH, LocalDate.of(2026, 12, 31)));

        assertThat(criada.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(criada.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(criada.getDescription()).isEqualTo("descricao");
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
        UUID id = criar("Busca").getId();

        Task achada = service.findById(id);

        assertThat(achada.getId()).isEqualTo(id);
        assertThat(achada.getTitle()).isEqualTo("Busca");
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
        service.changeStatus(criar("Concluida").getId(), TaskStatus.DONE);
        criar("Pendente");

        assertThat(service.findAll(TaskFilter.byStatus(TaskStatus.TODO)))
                .extracting(Task::getTitle)
                .containsExactly("Pendente");
        assertThat(service.findAll(TaskFilter.byStatus(TaskStatus.DONE)))
                .extracting(Task::getTitle)
                .containsExactly("Concluida");
    }

    @Test
    void filtrarPorPrioridadeRetornaSomenteODesejada() {
        criar("Alta", null, TaskPriority.HIGH, null);
        criar("Baixa", null, TaskPriority.LOW, null);

        assertThat(service.findAll(TaskFilter.byPriority(TaskPriority.HIGH)))
                .extracting(Task::getTitle)
                .containsExactly("Alta");
    }

    @Test
    void filtrosCompostosSeIntersecao() {
        service.changeStatus(criar("Alta pendente", null, TaskPriority.HIGH, null).getId(), TaskStatus.IN_PROGRESS);
        criar("Alta concluida", null, TaskPriority.HIGH, null);
        criar("Media pendente", null, TaskPriority.MEDIUM, null);

        List<Task> resultado = service.findAll(
                new TaskFilter(TaskStatus.IN_PROGRESS, TaskPriority.HIGH));

        assertThat(resultado).extracting(Task::getTitle).containsExactly("Alta pendente");
    }

    @Test
    void listarPaginadoRespeitaSizeEOrdenacao() {
        criar("A");
        criar("B");
        criar("C");

        Page<Task> primeira = service.list(
                TaskFilter.all(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title")));

        assertThat(primeira.getTotalElements()).isEqualTo(3);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.getContent()).extracting(Task::getTitle).containsExactly("A", "B");
    }

    // --- editar (RF-03) ---

    @Test
    void editarAtualizaConteudoEMantemStatus() {
        Task original = criar("Antigo", "antes", TaskPriority.LOW, null);
        service.changeStatus(original.getId(), TaskStatus.IN_PROGRESS);

        Task editada = service.update(original.getId(),
                new TaskCommand("  Novo  ", "depois", TaskPriority.HIGH, LocalDate.of(2026, 6, 1)));

        assertThat(editada.getTitle()).isEqualTo("Novo");
        assertThat(editada.getDescription()).isEqualTo("depois");
        assertThat(editada.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(editada.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(editada.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void tarefaPrincipalNaoAceitaTempoEstimado() {
        assertThatThrownBy(() -> service.create(
                new TaskCommand("T", null, null, null, 2.0, TimeUnit.DAYS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tempo estimado");
    }

    @Test
    void editarRaizComTempoEstimadoEhRecusado() {
        Task raiz = criar("Raiz");

        assertThatThrownBy(() -> service.update(raiz.getId(),
                new TaskCommand("Raiz", null, null, null, 2.0, TimeUnit.HOURS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tempo estimado");
    }

    @Test
    void subtarefaNaoAceitaPrazo() {
        Task raiz = criar("Raiz");

        assertThatThrownBy(() -> service.createSubtask(raiz.getId(),
                new TaskCommand("Filha", null, null, LocalDate.of(2026, 12, 1))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("prazo");
    }

    @Test
    void subtarefaComTempoEstimadoGravaESerializa() {
        Task raiz = criar("Raiz");
        Task filha = service.createSubtask(raiz.getId(),
                new TaskCommand("Filha", null, null, null, 2.0, TimeUnit.DAYS));
        assertThat(filha.getEstimatedTime()).isEqualTo(2.0);
        assertThat(filha.getEstimatedUnit()).isEqualTo(TimeUnit.DAYS);
        assertThat(filha.getDueDate()).isNull();

        Task editada = service.update(filha.getId(),
                new TaskCommand("Filha", null, null, null, 5.0, TimeUnit.HOURS));
        assertThat(editada.getEstimatedTime()).isEqualTo(5.0);
        assertThat(editada.getEstimatedUnit()).isEqualTo(TimeUnit.HOURS);

        Task vazia = service.update(filha.getId(),
                new TaskCommand("Filha", null, null, null, null, null));
        assertThat(vazia.getEstimatedTime()).isNull();
        assertThat(vazia.getEstimatedUnit()).isNull();
    }

    // --- aplicar sugestao (T-F13-02) ---

    @Test
    void aplicarSugestaoNaRaizSomaAsSubtarefasEmHoras() {
        Task raiz = criar("Raiz");
        service.createSubtask(raiz.getId(),
                new TaskCommand("A", null, null, null, 2.0, TimeUnit.HOURS));
        service.createSubtask(raiz.getId(),
                new TaskCommand("B", null, null, null, 1.0, TimeUnit.DAYS));

        Task aplicada = service.applySuggestion(raiz.getId(), TaskPriority.HIGH, 99.0);

        assertThat(aplicada.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(aplicada.getEstimatedTime()).isEqualTo(26.0);
        assertThat(aplicada.getEstimatedUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    void aplicarSugestaoNaRaizSemSubtarefasUsaAsHorasDaAnalise() {
        Task raiz = criar("Raiz");

        Task aplicada = service.applySuggestion(raiz.getId(), TaskPriority.MEDIUM, 4.0);

        assertThat(aplicada.getEstimatedTime()).isEqualTo(4.0);
        assertThat(aplicada.getEstimatedUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    void aplicarSugestaoSemHorasSoTrocaAPrioridade() {
        Task raiz = criar("Raiz");

        Task aplicada = service.applySuggestion(raiz.getId(), TaskPriority.HIGH, null);

        assertThat(aplicada.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(aplicada.getEstimatedTime()).isNull();
    }

    @Test
    void aplicarSugestaoNaSubtarefaUsaOEstimadoDaAnalise() {
        Task raiz = criar("Raiz");
        Task filha = service.createSubtask(raiz.getId(),
                new TaskCommand("Filha", null, null, null, 2.0, TimeUnit.HOURS));

        Task aplicada = service.applySuggestion(filha.getId(), TaskPriority.LOW, 5.0);

        assertThat(aplicada.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(aplicada.getEstimatedTime()).isEqualTo(5.0);
        assertThat(aplicada.getEstimatedUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    void editarTarefaInexistenteRetorna404() {
        assertThatThrownBy(() -> service.update(UUID.randomUUID(),
                new TaskCommand("X", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editarComTituloEmBrancoEhRecusado() {
        UUID id = criar("Original").getId();

        assertThatThrownBy(() -> service.update(id, new TaskCommand("  ", null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- status (RF-06) ---

    @Test
    void alterarStatusPersisteNoBanco() {
        UUID id = criar("Fluxo").getId();

        assertThat(service.changeStatus(id, TaskStatus.IN_PROGRESS).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.findById(id).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.changeStatus(id, TaskStatus.DONE).getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void alterarParaOMesmoStatusNaoFalhaNemMexeEmUpdatedAt() {
        UUID id = criar("Estavel").getId();
        // relê para comparar com o valor do banco: timestamptz guarda microssegundos
        Instant persistido = service.findById(id).getUpdatedAt();

        Task igual = service.changeStatus(id, TaskStatus.TODO);

        assertThat(igual.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(service.findById(id).getUpdatedAt()).isEqualTo(persistido);
    }

    @Test
    void voltarDeConcluidaParaEmAndamentoEhRecusado() {
        UUID id = criar("Concluida").getId();
        service.changeStatus(id, TaskStatus.DONE);

        assertThatThrownBy(() -> service.changeStatus(id, TaskStatus.IN_PROGRESS))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DONE");
    }

    @Test
    void reabrirDeConcluidaParaAFazerEhPermitido() {
        UUID id = criar("Concluida").getId();
        service.changeStatus(id, TaskStatus.DONE);

        assertThat(service.changeStatus(id, TaskStatus.TODO).getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void alterarStatusDeTarefaInexistenteRetorna404() {
        assertThatThrownBy(() -> service.changeStatus(UUID.randomUUID(), TaskStatus.DONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void concluirPaiComFlagConcluiSubtarefasPendentes() {
        Task pai = criar("Pai");
        Task pendente = criarSubtask(pai.getId(), "Filha pendente");
        Task emAndamento = criarSubtask(pai.getId(), "Filha em andamento");
        Task feita = criarSubtask(pai.getId(), "Filha feita");
        service.changeStatus(feita.getId(), TaskStatus.DONE);
        service.changeStatus(emAndamento.getId(), TaskStatus.IN_PROGRESS);

        Task paiConcluido = service.changeStatus(pai.getId(), TaskStatus.DONE, true);

        assertThat(paiConcluido.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(service.findById(pendente.getId()).getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(service.findById(emAndamento.getId()).getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(service.findById(feita.getId()).getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void concluirPaiSemFlagNaoMexeNasFilhas() {
        Task pai = criar("Pai");
        Task filha = criarSubtask(pai.getId(), "Filha");

        service.changeStatus(pai.getId(), TaskStatus.DONE, false);

        assertThat(service.findById(pai.getId()).getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(service.findById(filha.getId()).getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void flagComStatusDiferenteDeConcluidaNaoConcluiFilhas() {
        Task pai = criar("Pai");
        Task filha = criarSubtask(pai.getId(), "Filha");

        Task paiEmAndamento = service.changeStatus(pai.getId(), TaskStatus.IN_PROGRESS, true);

        assertThat(paiEmAndamento.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.findById(filha.getId()).getStatus()).isEqualTo(TaskStatus.TODO);
    }

    // --- excluir (RF-05) ---

    @Test
    void excluirRemoveATarefa() {
        UUID id = criar("Some").getId();

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
        Task pai = criar("Pai");
        criarSubtask(pai.getId(), "Filha 1");
        criarSubtask(pai.getId(), "Filha 2");

        service.delete(pai.getId());

        assertThat(service.findAll(TaskFilter.all())).isEmpty();
    }

    // --- subtarefas ---

    @Test
    void criarSubtaskLigaAoPai() {
        Task pai = criar("Pai");
        Task filha = criarSubtask(pai.getId(), "Filha");

        assertThat(filha.getParent().getId()).isEqualTo(pai.getId());
        assertThat(service.findSubtasks(pai.getId()))
                .extracting(Task::getTitle)
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
        assertThat(service.findSubtasks(criar("Solitaria").getId())).isEmpty();
    }

    // --- summary (RF-20) ---

    @Test
    void summaryComBancoVazioTemTudoZero() {
        assertThat(service.summary())
                .isEqualTo(new TaskSummary(0, 0, 0, 0, 0));
    }

    @Test
    void summaryContaCadaIndicador() {
        service.changeStatus(criar("P1").getId(), TaskStatus.DONE);
        service.changeStatus(criar("P2").getId(), TaskStatus.DONE);
        service.changeStatus(criar("Em1").getId(), TaskStatus.IN_PROGRESS);
        criar("Alta1", null, TaskPriority.HIGH, null);
        criar("Alta2", null, TaskPriority.HIGH, null);
        criar("P3");

        // pendentes = Alta1 + Alta2 + P3 (as duas HIGH continuam TODO)
        TaskSummary summary = service.summary();

        assertThat(summary.total()).isEqualTo(6);
        assertThat(summary.pending()).isEqualTo(3);
        assertThat(summary.inProgress()).isEqualTo(1);
        assertThat(summary.done()).isEqualTo(2);
        assertThat(summary.highPriority()).isEqualTo(2);
    }

    @Test
    void summarySomaPorStatusIgualAoTotal() {
        service.changeStatus(criar("A").getId(), TaskStatus.DONE);
        service.changeStatus(criar("B").getId(), TaskStatus.IN_PROGRESS);
        criar("C");
        criar("D", null, TaskPriority.HIGH, null);

        TaskSummary summary = service.summary();

        assertThat(summary.pending() + summary.inProgress() + summary.done())
                .isEqualTo(summary.total());
        assertThat(summary.highPriority()).isLessThanOrEqualTo(summary.total());
    }

    @Test
    void summaryContaSomenteeItensFinaisEAltaPrioridadeNaoConcluida() {
        Task pai = criar("Pai com filhas", null, TaskPriority.HIGH, null);
        criarSubtask(pai.getId(), "Filha 1");
        criarSubtask(pai.getId(), "Filha 2");
        service.changeStatus(pai.getId(), TaskStatus.DONE);
        Task altaConcluida = criar("Alta concluida", null, TaskPriority.HIGH, null);
        service.changeStatus(altaConcluida.getId(), TaskStatus.DONE);
        Task simples = criar("Simples", null, TaskPriority.HIGH, null);

        // Pai nao conta (tem filhas); contam Filha1, Filha2, altaConcluida e Simples.
        // Alta prioridade so a nao concluida: apenas Simples.
        TaskSummary summary = service.summary();

        assertThat(summary.total()).isEqualTo(4);
        assertThat(summary.pending()).isEqualTo(3);
        assertThat(summary.inProgress()).isZero();
        assertThat(summary.done()).isEqualTo(1);
        assertThat(summary.highPriority()).isEqualTo(1);
    }

    // --- lista so com tarefas-raiz (T-F07-01) ---

    @Test
    void listaPublicaNaoDevolveSubtarefas() {
        Task raiz = criar("Raiz");
        criarSubtask(raiz.getId(), "Filha");
        criar("Outra raiz");

        assertThat(service.list(TaskFilter.all(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(service.list(TaskFilter.all(), PageRequest.of(0, 10)).getContent())
                .extracting(Task::getTitle)
                .doesNotContain("Filha");
    }

    @Test
    void listaComFiltroTambemSoTrazRaizes() {
        Task raiz = criar("Raiz pendente");
        criarSubtask(raiz.getId(), "Filha pendente");

        assertThat(service.findAll(TaskFilter.byStatus(TaskStatus.TODO)))
                .extracting(Task::getTitle)
                .containsExactly("Raiz pendente");
        assertThat(service.findAll(null))
                .extracting(Task::getTitle)
                .doesNotContain("Filha pendente");
    }

    @Test
    void subtarefaContinuaAcessiveisPeloId() {
        Task raiz = criar("Raiz");
        Task filha = criarSubtask(raiz.getId(), "Filha");

        assertThat(service.findById(filha.getId()).getTitle()).isEqualTo("Filha");
        assertThat(service.findSubtasks(raiz.getId())).extracting(Task::getTitle).containsExactly("Filha");
    }

    @Test
    void subtaskCountsContaPorRaizNumaQuerySo() {
        Task raiz = criar("Raiz");
        Task outraRaiz = criar("Outra");
        criarSubtask(raiz.getId(), "Filha 1");
        criarSubtask(raiz.getId(), "Filha 2");

        assertThat(service.subtaskCounts(List.of(raiz.getId(), outraRaiz.getId())))
                .hasSize(1)
                .containsEntry(raiz.getId(), 2L)
                .doesNotContainKey(outraRaiz.getId());
        assertThat(service.subtaskCounts(List.of())).isEmpty();
    }

    // --- area de trabalho (F14) ---

    @Test
    void criarComAreaGravaOVinculo() {
        UUID areaId = areas.save(new WorkArea("Pessoal")).getId();

        Task task = service.create(new TaskCommand("Com area", null, null, null, null, null, areaId));

        assertThat(task.getArea().getId()).isEqualTo(areaId);
        assertThat(service.findById(task.getId()).getArea().getId()).isEqualTo(areaId);
    }

    @Test
    void subtarefaComAreaGravaOVinculo() {
        UUID areaId = areas.save(new WorkArea("Projeto")).getId();
        Task raiz = criar("Raiz");

        Task filha = service.createSubtask(raiz.getId(),
                new TaskCommand("Filha", null, null, null, null, null, areaId));

        assertThat(filha.getArea().getId()).isEqualTo(areaId);
    }

    @Test
    void editarComAreaNulaRemoveAVinculacao() {
        UUID areaId = areas.save(new WorkArea("A")).getId();
        Task task = service.create(new TaskCommand("T", null, null, null, null, null, areaId));

        Task atualizada = service.update(task.getId(),
                new TaskCommand("T", null, null, null, null, null, null));

        assertThat(atualizada.getArea()).isNull();
    }

    @Test
    void criarComAreaInexistenteEhRecusadoCom422() {
        assertThatThrownBy(() -> service.create(
                new TaskCommand("T", null, null, null, null, null, UUID.randomUUID())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("area de trabalho nao encontrada");
    }

    @Test
    void filtroPorAreaTrazSomenteAsTarefasDaArea() {
        UUID areaA = areas.save(new WorkArea("A")).getId();
        UUID areaB = areas.save(new WorkArea("B")).getId();
        service.create(new TaskCommand("NA", null, null, null, null, null, areaA));
        service.create(new TaskCommand("NB", null, null, null, null, null, areaB));

        assertThat(service.findAll(new TaskFilter(null, null, areaA)))
                .extracting(Task::getTitle)
                .containsExactly("NA");
    }

    @Test
    void filtroPorTituloTrazSomenteAsQueContemIgnorandoCaixa() {
        service.create(new TaskCommand("Pintar a parede", null, null, null, null, null, null));
        service.create(new TaskCommand("Comprar tinta", null, null, null, null, null, null));
        service.create(new TaskCommand("Lavar o carro", null, null, null, null, null, null));

        assertThat(service.findAll(new TaskFilter(null, null, null, "tINTA")))
                .extracting(Task::getTitle)
                .containsExactly("Comprar tinta");
    }

    @Test
    void filtroPorTituloTrataCoringaDoLikeComoLiteral() {
        service.create(new TaskCommand("100% feito", null, null, null, null, null, null));
        service.create(new TaskCommand("100 e feito", null, null, null, null, null, null));

        assertThat(service.findAll(new TaskFilter(null, null, null, "%")))
                .extracting(Task::getTitle)
                .containsExactly("100% feito");
    }

    @Test
    void filtroPorTituloCompostaComStatus() {
        service.create(new TaskCommand("Relatorio pronto", null, null, null, null, null, null));
        Task emAberto = service.create(
                new TaskCommand("Relatorio em andamento", null, null, null, null, null, null));

        emAberto.changeStatus(TaskStatus.IN_PROGRESS);
        repository.save(emAberto);

        assertThat(service.findAll(
                new TaskFilter(TaskStatus.IN_PROGRESS, null, null, "relatorio")))
                .extracting(Task::getTitle)
                .containsExactly("Relatorio em andamento");
    }

    // --- helpers ---

    private static TaskCommand pedido(String title, String description, TaskPriority priority,
            LocalDate dueDate) {
        return new TaskCommand(title, description, priority, dueDate, null, null);
    }

    private Task criar(String title) {
        return criar(title, null, null, null);
    }

    private Task criar(String title, String description, TaskPriority priority, LocalDate dueDate) {
        return service.create(pedido(title, description, priority, dueDate));
    }

    private Task criarSubtask(UUID parentId, String title) {
        return service.createSubtask(parentId, pedido(title, null, null, null));
    }
}