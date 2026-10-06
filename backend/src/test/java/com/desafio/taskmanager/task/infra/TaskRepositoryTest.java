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
                "Escrever README", "detalhar as 7 secoes", TaskPriority.ALTA,
                LocalDate.of(2026, 12, 31), null));

        Task found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Escrever README");
        assertThat(found.getDescription()).isEqualTo("detalhar as 7 secoes");
        assertThat(found.getStatus()).isEqualTo(TaskStatus.A_FAZER);
        assertThat(found.getPriority()).isEqualTo(TaskPriority.ALTA);
        assertThat(found.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(found.getParent()).isNull();
        assertThat(found.isSubtask()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void tarefaNovaNasceComStatusInicialEPrioridadePadrao() {
        Task saved = repository.saveAndFlush(new Task("Sem prioridade", null, null, null, null));

        assertThat(saved.getStatus()).isEqualTo(TaskStatus.A_FAZER);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.MEDIA);
        assertThat(saved.getDueDate()).isNull();
    }

    @Test
    void atualizaStatusNoBanco() {
        Task task = repository.saveAndFlush(new Task("Migrar schema", null, null, null, null));

        task.changeStatus(TaskStatus.EM_ANDAMENTO);
        repository.saveAndFlush(task);
        task.changeStatus(TaskStatus.CONCLUIDA);
        repository.saveAndFlush(task);

        Task found = repository.findById(task.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TaskStatus.CONCLUIDA);
    }

    @Test
    void filtraPorStatus() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.A_FAZER),
                nova("B", TaskStatus.EM_ANDAMENTO),
                nova("C", TaskStatus.CONCLUIDA)));

        assertThat(repository.findByStatusOrderByCreatedAtDesc(TaskStatus.EM_ANDAMENTO))
                .extracting(Task::getTitle)
                .containsExactly("B");
    }

    @Test
    void filtraPorListaDeStatusParaFerramentasDoAssistente() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.A_FAZER),
                nova("B", TaskStatus.EM_ANDAMENTO),
                nova("C", TaskStatus.CONCLUIDA)));

        assertThat(repository.findByStatusInOrderByCreatedAtDesc(
                List.of(TaskStatus.A_FAZER, TaskStatus.EM_ANDAMENTO)))
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void filtraPorPrioridade() {
        repository.saveAllAndFlush(List.of(
                new Task("A", null, TaskPriority.BAIXA, null, null),
                new Task("B", null, TaskPriority.ALTA, null, null)));

        assertThat(repository.findByPriorityAndStatusInOrderByCreatedAtDesc(
                TaskPriority.ALTA, List.of(TaskStatus.A_FAZER, TaskStatus.EM_ANDAMENTO, TaskStatus.CONCLUIDA)))
                .extracting(Task::getTitle)
                .containsExactly("B");
    }

    @Test
    void filtraComSpecificationCompostaEOrdena() {
        repository.saveAllAndFlush(List.of(
                new Task("A", null, TaskPriority.ALTA, null, null),
                new Task("B", null, TaskPriority.BAIXA, null, null),
                new Task("C", null, TaskPriority.ALTA, null, null)));

        Specification<Task> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("priority"), TaskPriority.ALTA),
                cb.equal(root.get("status"), TaskStatus.A_FAZER));

        Page<Task> page = repository.findAll(spec,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("A", "C");
    }

    @Test
    void paginaResultados() {
        repository.saveAllAndFlush(List.of(
                nova("A", TaskStatus.A_FAZER),
                nova("B", TaskStatus.A_FAZER),
                nova("C", TaskStatus.CONCLUIDA)));

        Page<Task> firstPage = repository.findByStatus(TaskStatus.A_FAZER, PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    @Test
    void buscaSubtarefasPeloPai() {
        Task parent = repository.saveAndFlush(nova("Tarefa pai", TaskStatus.EM_ANDAMENTO));
        Task other = repository.saveAndFlush(nova("Outra tarefa", TaskStatus.A_FAZER));

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
        Task parent = repository.saveAndFlush(nova("Tarefa pai", TaskStatus.A_FAZER));
        Task sub = repository.saveAndFlush(new Task("Sub", null, null, null, parent));

        Task found = repository.findById(sub.getId()).orElseThrow();

        assertThat(found.isSubtask()).isTrue();
        assertThat(found.getParent().getId()).isEqualTo(parent.getId());
    }

    @Test
    void deletaComSubtarefasEmCascata() {
        Task parent = repository.saveAndFlush(nova("Tarefa pai", TaskStatus.A_FAZER));
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
                new Task("A", null, TaskPriority.ALTA, null, null),
                new Task("B", null, TaskPriority.ALTA, null, null),
                new Task("C", null, TaskPriority.BAIXA, null, null)));

        repository.saveAndFlush(nova("D", TaskStatus.CONCLUIDA));

        assertThat(repository.countAll()).isEqualTo(4);
        assertThat(repository.countByStatusValue(TaskStatus.A_FAZER)).isEqualTo(3);
        assertThat(repository.countByStatusValue(TaskStatus.CONCLUIDA)).isEqualTo(1);
        assertThat(repository.countByPriorityValue(TaskPriority.ALTA)).isEqualTo(2);
        assertThat(repository.countByPriorityValueAndNotDone(TaskPriority.ALTA, TaskStatus.CONCLUIDA))
                .isEqualTo(2);
    }

    @Test
    void consultaPorPrazoParaFerramentasDoAssistente() {
        LocalDate hoje = LocalDate.now();
        repository.saveAllAndFlush(List.of(
                new Task("Vencida", null, null, hoje.minusDays(3), null),
                new Task("Vence hoje", null, null, hoje, null),
                new Task("Futura", null, null, hoje.plusDays(10), null)));

        assertThat(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(hoje, TaskStatus.CONCLUIDA))
                .extracting(Task::getTitle)
                .containsExactly("Vencida");

        assertThat(repository.findByDueDateBetweenOrderByDueDateAsc(hoje, hoje.plusDays(7)))
                .extracting(Task::getTitle)
                .containsExactly("Vence hoje");
    }

    @Test
    void tarefasSemPaiSaoAsDeTopo() {
        Task parent = repository.saveAndFlush(nova("Pai", TaskStatus.A_FAZER));
        UUID subId = repository.saveAndFlush(new Task("Sub", null, null, null, parent)).getId();

        assertThat(repository.findByIdAndParentIsNull(subId)).isEmpty();
        assertThat(repository.findByIdAndParentIsNull(parent.getId())).isPresent();
    }

    private Task nova(String title, TaskStatus status) {
        Task task = new Task(title, null, null, null, null);
        if (status != TaskStatus.A_FAZER) {
            task.changeStatus(status);
        }
        return task;
    }
}