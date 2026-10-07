package com.desafio.taskmanager.assistant.application.tools;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.desafio.taskmanager.assistant.application.tools.dto.TaskToolResult;
import com.desafio.taskmanager.common.config.AssistantLimitsProperties;
import com.desafio.taskmanager.common.error.BusinessRuleException;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;
import com.desafio.taskmanager.task.infra.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ferramentas somente-leitura do assistente (RF-19, RNF-13) com o repositorio
 * mockado: filtros certos, limites aplicados, id inexistente sem erro e nenhuma
 * chamada a metodo de escrita.
 */
@ExtendWith(MockitoExtension.class)
class TaskQueryToolsTest {

    @Mock
    private TaskRepository repository;

    private TaskQueryTools tools;

    @BeforeEach
    void montaFerramentas() {
        tools = new TaskQueryTools(repository, new AssistantLimitsProperties(10, true));
    }

    @Test
    void getPendingTasksConsultasStatusEmAberto() {
        when(repository.findByStatusInOrderByCreatedAtDesc(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)))
                .thenReturn(List.of(
                        tarefa("T1", TaskStatus.IN_PROGRESS, TaskPriority.HIGH),
                        tarefa("T2", TaskStatus.TODO, TaskPriority.MEDIUM)));

        List<TaskToolResult> resultados = tools.getPendingTasks();

        assertThat(resultados).extracting(TaskToolResult::title)
                .containsExactly("T1", "T2");
        assertThat(resultados).extracting(TaskToolResult::status)
                .containsExactly(TaskStatus.IN_PROGRESS, TaskStatus.TODO);
        verify(repository).findByStatusInOrderByCreatedAtDesc(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS));
    }

    @Test
    void resultadoRespeitaOLimiteConfigurado() {
        tools = new TaskQueryTools(repository, new AssistantLimitsProperties(3, true));
        List<Task> tarefas = List.of(
                tarefa("1", TaskStatus.TODO, TaskPriority.MEDIUM),
                tarefa("2", TaskStatus.TODO, TaskPriority.MEDIUM),
                tarefa("3", TaskStatus.TODO, TaskPriority.MEDIUM),
                tarefa("4", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM),
                tarefa("5", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM));
        when(repository.findByStatusInOrderByCreatedAtDesc(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)))
                .thenReturn(tarefas);

        assertThat(tools.getPendingTasks()).hasSize(3);
    }

    @Test
    void getOverdueTasksUsaPrazoDeHojeComoFronteira() {
        LocalDate hoje = LocalDate.now();
        when(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(hoje, TaskStatus.DONE))
                .thenReturn(List.of(tarefa("Vencida", TaskStatus.TODO, TaskPriority.HIGH)));

        assertThat(tools.getOverdueTasks()).extracting(TaskToolResult::title)
                .containsExactly("Vencida");
        verify(repository).findByDueDateLessThanAndStatusNotOrderByDueDateAsc(
                eq(hoje), eq(TaskStatus.DONE));
    }

    @Test
    void getTaskByIdDevolveVazioParaIdInexistente() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThat(tools.getTaskById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getTaskByIdMapeiaAEntidadeParaODto() {
        Task tarefa = tarefa("Recurso", TaskStatus.IN_PROGRESS, TaskPriority.HIGH);
        when(repository.findById(any())).thenReturn(Optional.of(tarefa));

        TaskToolResult resultado = tools.getTaskById(tarefa.getId()).orElseThrow();

        assertThat(resultado.id()).isEqualTo(tarefa.getId());
        assertThat(resultado.title()).isEqualTo("Recurso");
        assertThat(resultado.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(resultado.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(resultado.dueDate()).isNull();
    }

    @Test
    void getTasksByPriorityFiltraPelaPrioridadeInformada() {
        when(repository.findByPriorityAndStatusInOrderByCreatedAtDesc(
                eq(TaskPriority.HIGH),
                eq(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE))))
                .thenReturn(List.of(tarefa("Critica", TaskStatus.TODO, TaskPriority.HIGH)));

        assertThat(tools.getTasksByPriority(TaskPriority.HIGH))
                .extracting(TaskToolResult::title)
                .containsExactly("Critica");
        verify(repository).findByPriorityAndStatusInOrderByCreatedAtDesc(
                eq(TaskPriority.HIGH),
                eq(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE)));
    }

    @Test
    void getTasksDueSoonUsaJanelaDeHojeAteHojeMaisDias() {
        LocalDate hoje = LocalDate.now();
        when(repository.findByDueDateBetweenOrderByDueDateAsc(hoje, hoje.plusDays(7)))
                .thenReturn(List.of(tarefa("Quase", TaskStatus.TODO, TaskPriority.MEDIUM)));

        assertThat(tools.getTasksDueSoon(7)).extracting(TaskToolResult::title)
                .containsExactly("Quase");
        verify(repository).findByDueDateBetweenOrderByDueDateAsc(eq(hoje), eq(hoje.plusDays(7)));
    }

    @Test
    void getTasksDueSoonRejeitaDiasForaDaJanela() {
        for (int days : Arrays.asList(0, -1, 366, 999)) {
            assertThatThrownBy(() -> tools.getTasksDueSoon(days))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("days deve estar entre 1 e 365");
        }
    }

    @Test
    void getTaskSummaryMontaAsContagens() {
        when(repository.countAll()).thenReturn(4L);
        when(repository.countByStatusValue(TaskStatus.TODO)).thenReturn(2L);
        when(repository.countByStatusValue(TaskStatus.IN_PROGRESS)).thenReturn(1L);
        when(repository.countByStatusValue(TaskStatus.DONE)).thenReturn(1L);
        when(repository.countByPriorityValue(TaskPriority.HIGH)).thenReturn(2L);

        assertThat(tools.getTaskSummary()).isEqualTo(
                new TaskSummary(4L, 2L, 1L, 1L, 2L));
    }

    @Test
    void nenhumMetodoDeEscritaDoRepositorioEhChamado() {
        when(repository.findByStatusInOrderByCreatedAtDesc(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)))
                .thenReturn(List.of());
        when(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(
                LocalDate.now(), TaskStatus.DONE))
                .thenReturn(List.of());
        when(repository.findByDueDateBetweenOrderByDueDateAsc(LocalDate.now(), LocalDate.now().plusDays(1)))
                .thenReturn(List.of());
        when(repository.findByPriorityAndStatusInOrderByCreatedAtDesc(
                any(), any()))
                .thenReturn(List.of());
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.countAll()).thenReturn(0L);
        when(repository.countByStatusValue(eq(TaskStatus.TODO))).thenReturn(0L);
        when(repository.countByStatusValue(eq(TaskStatus.IN_PROGRESS))).thenReturn(0L);
        when(repository.countByStatusValue(eq(TaskStatus.DONE))).thenReturn(0L);
        when(repository.countByPriorityValue(eq(TaskPriority.HIGH))).thenReturn(0L);

        tools.getPendingTasks();
        tools.getOverdueTasks();
        tools.getTasksDueSoon(1);
        tools.getTasksByPriority(TaskPriority.HIGH);
        tools.getTaskById(UUID.randomUUID());
        tools.getTaskSummary();

        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
        verify(repository, never()).saveAll(anyIterable());
        verify(repository, never()).delete(any(Task.class));
        verify(repository, never()).deleteById(any());
        verify(repository, never()).deleteAll();
        verify(repository, never()).flush();
    }

    private static Task tarefa(String titulo, TaskStatus status, TaskPriority prioridade) {
        Task tarefa = new Task(titulo, null, prioridade, null, null);
        if (status == TaskStatus.IN_PROGRESS) {
            tarefa.changeStatus(TaskStatus.IN_PROGRESS);
        } else if (status == TaskStatus.DONE) {
            tarefa.changeStatus(TaskStatus.DONE);
        }
        return tarefa;
    }
}