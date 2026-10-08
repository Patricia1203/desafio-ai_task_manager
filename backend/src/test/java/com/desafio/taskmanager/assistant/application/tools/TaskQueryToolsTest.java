package com.desafio.taskmanager.assistant.application.tools;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.desafio.taskmanager.assistant.application.tools.dto.TaskToolResult;
import com.desafio.taskmanager.assistant.application.tools.dto.ToolResultPage;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

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
 * mockado: filtros certos, limites aplicados com o total real, ordem de
 * urgencia repassada ao banco, id inexistente sem erro e nenhuma chamada a
 * metodo de escrita.
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
    void getPendingTasksConsultaStatusEmAbertoPorUrgencia() {
        when(repository.countByStatusIn(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)))
                .thenReturn(2L);
        when(repository.findEmAbertoPorUrgencia(any(), any(), any(), any())).thenReturn(List.of(
                tarefa("T1", TaskStatus.IN_PROGRESS, TaskPriority.HIGH),
                tarefa("T2", TaskStatus.TODO, TaskPriority.MEDIUM)));

        ToolResultPage pagina = tools.getPendingTasks();

        assertThat(pagina.items()).extracting(TaskToolResult::title)
                .containsExactly("T1", "T2");
        assertThat(pagina.items()).extracting(TaskToolResult::status)
                .containsExactly(TaskStatus.IN_PROGRESS, TaskStatus.TODO);
        verify(repository).findEmAbertoPorUrgencia(
                eq(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)),
                eq(TaskPriority.HIGH), eq(TaskPriority.MEDIUM), any(Pageable.class));
    }

    @Test
    void oTotalVemDoFiltroInteiroEOsItensRespeitamOLimite() {
        tools = new TaskQueryTools(repository, new AssistantLimitsProperties(3, true));
        when(repository.countByStatusIn(any())).thenReturn(40L);
        when(repository.findEmAbertoPorUrgencia(any(), any(), any(), any())).thenReturn(List.of(
                tarefa("1", TaskStatus.TODO, TaskPriority.MEDIUM),
                tarefa("2", TaskStatus.TODO, TaskPriority.MEDIUM),
                tarefa("3", TaskStatus.TODO, TaskPriority.MEDIUM),
                tarefa("4", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM),
                tarefa("5", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM)));

        ToolResultPage pagina = tools.getPendingTasks();

        // O total e do filtro inteiro: e o que faltava para a resposta nao mentir.
        assertThat(pagina.total()).isEqualTo(40L);
        assertThat(pagina.items()).hasSize(5);
    }

    @Test
    void oLimiteVaiParaOBancoComOPageable() {
        tools = new TaskQueryTools(repository, new AssistantLimitsProperties(7, true));
        when(repository.countByStatusIn(any())).thenReturn(0L);
        when(repository.findEmAbertoPorUrgencia(any(), any(), any(), any())).thenReturn(List.of());

        tools.getPendingTasks();

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findEmAbertoPorUrgencia(any(), any(), any(), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(7);
    }

    @Test
    void getOverdueTasksUsaPrazoDeHojeComoFronteira() {
        LocalDate hoje = LocalDate.now();
        when(repository.countByDueDateLessThanAndStatusNot(hoje, TaskStatus.DONE)).thenReturn(1L);
        when(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(any(), any(), any()))
                .thenReturn(List.of(tarefa("Vencida", TaskStatus.TODO, TaskPriority.HIGH)));

        ToolResultPage pagina = tools.getOverdueTasks();

        assertThat(pagina.total()).isEqualTo(1L);
        assertThat(pagina.items()).extracting(TaskToolResult::title).containsExactly("Vencida");
        verify(repository).findByDueDateLessThanAndStatusNotOrderByDueDateAsc(
                eq(hoje), eq(TaskStatus.DONE), any(Pageable.class));
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
    void getTasksByPriorityFiltraPelaPrioridadeInformadaComTotal() {
        when(repository.countByPriorityValue(TaskPriority.HIGH)).thenReturn(7L);
        when(repository.findPorPrioridadePorUrgencia(any(), any(), any(), any()))
                .thenReturn(List.of(tarefa("Urgente", TaskStatus.TODO, TaskPriority.HIGH)));

        ToolResultPage pagina = tools.getTasksByPriority(TaskPriority.HIGH);

        assertThat(pagina.total()).isEqualTo(7L);
        assertThat(pagina.items()).extracting(TaskToolResult::title).containsExactly("Urgente");
        verify(repository).findPorPrioridadePorUrgencia(eq(TaskPriority.HIGH), eq(TaskPriority.HIGH),
                eq(TaskPriority.MEDIUM), any(Pageable.class));
    }

    @Test
    void getTasksDueSoonUsaJanelaDeHojeAteHojeMaisDias() {
        LocalDate hoje = LocalDate.now();
        when(repository.countByDueDateBetween(hoje, hoje.plusDays(7))).thenReturn(2L);
        when(repository.findByDueDateBetweenOrderByDueDateAsc(any(), any(), any()))
                .thenReturn(List.of(tarefa("Quase", TaskStatus.TODO, TaskPriority.MEDIUM)));

        ToolResultPage pagina = tools.getTasksDueSoon(7);

        assertThat(pagina.total()).isEqualTo(2L);
        assertThat(pagina.items()).extracting(TaskToolResult::title).containsExactly("Quase");
        verify(repository).findByDueDateBetweenOrderByDueDateAsc(
                eq(hoje), eq(hoje.plusDays(7)), any(Pageable.class));
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
        when(repository.countLeaves()).thenReturn(4L);
        when(repository.countLeavesByStatusValue(TaskStatus.TODO)).thenReturn(2L);
        when(repository.countLeavesByStatusValue(TaskStatus.IN_PROGRESS)).thenReturn(1L);
        when(repository.countLeavesByStatusValue(TaskStatus.DONE)).thenReturn(1L);
        when(repository.countLeavesByPriorityValueAndNotDone(TaskPriority.HIGH, TaskStatus.DONE))
                .thenReturn(2L);

        assertThat(tools.getTaskSummary()).isEqualTo(
                new TaskSummary(4L, 2L, 1L, 1L, 2L));
    }

    @Test
    void nenhumMetodoDeEscritaDoRepositorioEhChamado() {
        when(repository.countByStatusIn(any())).thenReturn(0L);
        when(repository.findEmAbertoPorUrgencia(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.countByDueDateLessThanAndStatusNot(any(), any())).thenReturn(0L);
        when(repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(repository.countByDueDateBetween(any(), any())).thenReturn(0L);
        when(repository.findByDueDateBetweenOrderByDueDateAsc(any(), any(), any())).thenReturn(List.of());
        when(repository.countByPriorityValue(any())).thenReturn(0L);
        when(repository.findPorPrioridadePorUrgencia(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.countLeaves()).thenReturn(0L);
        when(repository.countLeavesByStatusValue(eq(TaskStatus.TODO))).thenReturn(0L);
        when(repository.countLeavesByStatusValue(eq(TaskStatus.IN_PROGRESS))).thenReturn(0L);
        when(repository.countLeavesByStatusValue(eq(TaskStatus.DONE))).thenReturn(0L);
        when(repository.countLeavesByPriorityValueAndNotDone(eq(TaskPriority.HIGH), eq(TaskStatus.DONE)))
                .thenReturn(0L);

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