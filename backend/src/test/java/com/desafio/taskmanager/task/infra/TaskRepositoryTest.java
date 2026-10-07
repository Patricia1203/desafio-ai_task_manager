package com.desafio.taskmanager.task.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

import com.desafio.taskmanager.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-04 contra Postgres real: a entidade Task precisa mapear exatamente o que a
 * migration V2 criou, sob ddl-auto=validate. Qualquer divergência de coluna ou de
 * tipo quebra o boot do contexto, não só um assert.
 *
 * <p>O container vem de {@link PostgresIntegrationTest}: o banco é compartilhado
 * com as outras classes de integração e o {@code @BeforeEach} limpa a tabela.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class TaskRepositoryTest extends PostgresIntegrationTest {

    /** "Todos os itens": as queries de urgency nao limitam aqui. */
    private static final Pageable TODOS = PageRequest.of(0, 100);

    @Autowired
    private TaskRepository repository;

    /**
     * Os testesDividem o mesmo container, entao a tabela e limpa antes de cada um.
     * Sem isso as contagens dependeriam da ordem de execucao.
     */
    @BeforeEach
    void limpaTabela() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    void gravaELeComTodosOsCampos() {
        Task saved = repository.saveAndFlush(new Task(
                "Escrever README", "detalhar as 7 secoes", TaskPriority.HIGH,
                LocalDate.of(2026, 12, 31), null));

        Task found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Escrever README");
        assertThat(found.getDescription()).isEqualTo("detalhar as 7 secoes");
        assertThat(found.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(found.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(found.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(found.getParent()).isNull();
        assertThat(found.isSubtask()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void tarefaNovaNasceComStatusInicialEPrioridadePadrao() {
        Task saved = repository.saveAndFlush(new Task("Sem prioridade", null, null, null, null));

        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(saved.getDueDate()).isNull();
    }

    @Test
    void atualizaStatusNoBanco() {
        Task task = repository.saveAndFlush(new Task("Migrar schema", null, null, null, null));

        task.changeStatus(TaskStatus.IN_PROGRESS);
        repository.saveAndFlush(task);
        task.changeStatus(TaskStatus.DONE);
        repository.saveAndFlush(task);

        Task found = repository.findById(task.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void filtraPorStatus() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.TODO),
                nova("B", TaskStatus.IN_PROGRESS),
                nova("C", TaskStatus.DONE)));

        assertThat(repository.findByStatusOrderByCreatedAtDesc(TaskStatus.IN_PROGRESS))
                .extracting(Task::getTitle)
                .containsExactly("B");
    }

    @Test
    void filtraPorListaDeStatusParaFerramentasDoAssistente() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.TODO),
                nova("B", TaskStatus.IN_PROGRESS),
                nova("C", TaskStatus.DONE)));

        assertThat(repository.findEmAbertoPorUrgencia(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
                TaskPriority.HIGH, TaskPriority.MEDIUM, TODOS))
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void filtraPorPrioridade() {
        repository.saveAllAndFlush(List.of(
                new Task("A", null, TaskPriority.LOW, null, null),
                new Task("B", null, TaskPriority.HIGH, null, null)));

        assertThat(repository.findPorPrioridadePorUrgencia(TaskPriority.HIGH, TODOS))
                .extracting(Task::getTitle)
                .containsExactly("B");
    }

    @Test
    void ordenaEmAbertoPorUrgenciaPrazoPrioridadeERecencia() {
        LocalDate hoje = LocalDate.now();
        repository.saveAllAndFlush(List.of(
                comVencimento("Sem prazo", null, TaskPriority.HIGH),
                comVencimento("Prazo distante", hoje.plusDays(30), TaskPriority.HIGH),
                comVencimento("Prazo proximo LOW", hoje.plusDays(1), TaskPriority.LOW),
                comVencimento("Prazo proximo HIGH", hoje.plusDays(1), TaskPriority.HIGH),
                comVencimento("Prazo medio", hoje.plusDays(10), TaskPriority.MEDIUM)));

        List<Task> emAberto = repository.findEmAbertoPorUrgencia(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
                TaskPriority.HIGH, TaskPriority.MEDIUM, TODOS);

        // Sem prazo por ultimo; no mesmo prazo, HIGH antes de LOW; prazo antes de prioridade.
        assertThat(emAberto).extracting(Task::getTitle).containsExactly(
                "Prazo proximo HIGH", "Prazo proximo LOW", "Prazo medio",
                "Prazo distante", "Sem prazo");
    }

    @Test
    void contaOFilterInteiroParaOEnvelopeDaFerramenta() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.TODO),
                nova("B", TaskStatus.IN_PROGRESS),
                nova("C", TaskStatus.DONE)));

        assertThat(repository.countByStatusIn(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)))
                .isEqualTo(2L);
    }

    @Test
    void filtraComSpecificationCompostaEOrdena() {
        repository.saveAllAndFlush(List.of(
                new Task("A", null, TaskPriority.HIGH, null, null),
                new Task("B", null, TaskPriority.LOW, null, null),
                new Task("C", null, TaskPriority.HIGH, null, null)));

        Specification<Task> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("priority"), TaskPriority.HIGH),
                cb.equal(root.get("status"), TaskStatus.TODO));

        Page<Task> page = repository.findAll(spec,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("A", "C");
    }

    @Test
    void paginaResultados() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.TODO),
                nova("B", TaskStatus.TODO),
                nova("C", TaskStatus.DONE)));

        Page<Task> firstPage = repository.findByStatus(TaskStatus.TODO, PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    @Test
    void buscaSubtarefasPeloPai() {
        Task parent = repository.saveAndFlush(nova("Tarefa pai", TaskStatus.IN_PROGRESS));
        Task other = repository.saveAndFlush(nova("Outra tarefa", TaskStatus.TODO));

        repository.saveAllAndFlush(List.of(
                new Task("Sub 1", null, null, null, parent),
                new Task("Sub 2", null, null, null, parent),
                new Task("Sub de outra", null, null, null, other)));

        assertThat(repository.findByParentIdOrderByCreatedAtAsc(parent.getId()))
                .extracting(Task::getTitle)
                .containsExactly("Sub 1", "Sub 2");
    }

    @Test
    void subtarefaApontaParaOPai() {
        Task parent = repository.saveAndFlush(nova("Tarefa pai", TaskStatus.TODO));
        Task sub = repository.saveAndFlush(new Task("Sub", null, null, null, parent));

        Task found = repository.findById(sub.getId()).orElseThrow();

        assertThat(found.isSubtask()).isTrue();
        assertThat(found.getParent().getId()).isEqualTo(parent.getId());
    }

    @Test
    void deletaComSubtarefasEmCascata() {
        Task parent = repository.saveAndFlush(nova("Tarefa pai", TaskStatus.TODO));
        Task sub = repository.saveAndFlush(new Task("Sub", null, null, null, parent));

        repository.deleteById(parent.getId());
        repository.flush();

        assertThat(repository.findById(parent.getId())).isEmpty();
        assertThat(repository.findById(sub.getId())).isEmpty();
        assertThat(repository.countSubtasks()).isZero();
    }

    @Test
    void contaPorStatusEPrioridadeParaOSummary() {
        repository.saveAllAndFlush(List.of(
                new Task("A", null, TaskPriority.HIGH, null, null),
                new Task("B", null, TaskPriority.HIGH, null, null),
                new Task("C", null, TaskPriority.LOW, null, null)));

        repository.saveAndFlush(nova("D", TaskStatus.DONE));

        assertThat(repository.countAll()).isEqualTo(4);
        assertThat(repository.countByStatusValue(TaskStatus.TODO)).isEqualTo(3);
        assertThat(repository.countByStatusValue(TaskStatus.DONE)).isEqualTo(1);
        assertThat(repository.countByPriorityValue(TaskPriority.HIGH)).isEqualTo(2);
        assertThat(repository.countByPriorityValueAndNotDone(TaskPriority.HIGH, TaskStatus.DONE))
                .isEqualTo(2);
    }

    @Test
    void consultaPorPrazoParaFerramentasDoAssistente() {
        LocalDate hoje = LocalDate.now();
        repository.saveAllAndFlush(List.of(
                new Task("Vencida", null, null, hoje.minusDays(3), null),
                new Task("Vence hoje", null, null, hoje, null),
                new Task("Futura", null, null, hoje.plusDays(10), null)));

        assertThat(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(hoje, TaskStatus.DONE, TODOS))
                .extracting(Task::getTitle)
                .containsExactly("Vencida");

        assertThat(repository.findByDueDateBetweenOrderByDueDateAsc(hoje, hoje.plusDays(7), TODOS))
                .extracting(Task::getTitle)
                .containsExactly("Vence hoje");
    }

    @Test
    void tarefasSemPaiSaoAsDeTopo() {
        Task parent = repository.saveAndFlush(nova("Pai", TaskStatus.TODO));
        UUID subId = repository.saveAndFlush(new Task("Sub", null, null, null, parent)).getId();

        assertThat(repository.findByIdAndParentIsNull(subId)).isEmpty();
        assertThat(repository.findByIdAndParentIsNull(parent.getId())).isPresent();
    }

    private static Task comVencimento(String title, LocalDate dueDate, TaskPriority prioridade) {
        return new Task(title, null, prioridade, dueDate, null);
    }

    private Task nova(String title, TaskStatus status) {
        Task task = new Task(title, null, null, null, null);
        if (status != TaskStatus.TODO) {
            task.changeStatus(status);
        }
        return task;
    }
}