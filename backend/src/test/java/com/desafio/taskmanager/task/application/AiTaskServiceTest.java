package com.desafio.taskmanager.task.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.desafio.taskmanager.ai.port.TaskAiPort;
import com.desafio.taskmanager.ai.port.dto.ProposedSubtask;
import com.desafio.taskmanager.ai.port.dto.TaskAiContext;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskComplexity;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.area.infra.WorkAreaRepository;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TimeUnit;
import com.desafio.taskmanager.task.infra.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negocio da F03 contra a porta fake: improve e decompose nao
 * persistem, analyze nao altera a tarefa e apply cria exatamente as subtarefas
 * recebidas sob o pai. O contrato do JSON e o mapeamento de erros sao teste do
 * {@code AiTaskControllerTest}.
 */
class AiTaskServiceTest {

    private TaskRepository repository;
    private IaFake ia;
    private AiTaskService service;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        ia = new IaFake();
        service = new AiTaskService(ia, new TaskService(repository, mock(WorkAreaRepository.class)));
        when(repository.save(any(Task.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static Task tarefa() {
        return new Task("Titulo", "Descricao", TaskPriority.LOW, null, null);
    }

    @Test
    void improveDevolveASugestaoSemGravarNada() {
        TaskImprovement sugestao = service.improve("Titulo", "Descricao");

        assertThat(sugestao.title()).isEqualTo("Titulo melhorado");
        assertThat(ia.contextos.get(0))
                .isEqualTo(new TaskAiContext("Titulo", "Descricao", TaskPriority.DEFAULT));
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void improveComDescricaoNulaMandaTextoVazioParaOPrompt() {
        service.improve("Titulo", null);

        assertThat(ia.contextos.get(0).description()).isEmpty();
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void analyzeDevolveAAnaliseENaoAlteraAPrioridadeDaTarefa() {
        Task tarefa = tarefa();
        when(repository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

        TaskAnalysis analise = service.analyze(tarefa.getId());

        assertThat(analise.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(tarefa.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(ia.contextos.get(0))
                .isEqualTo(new TaskAiContext("Titulo", "Descricao", TaskPriority.LOW));
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void analyzeDeIdInexistenteEh404SemGravar() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void decomposeDevolveASugestaoSemCriarSubtarefa() {
        Task tarefa = tarefa();
        when(repository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

        TaskDecomposition decomposicao = service.decompose(tarefa.getId());

        assertThat(decomposicao.subtasks()).hasSize(2);
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void applyCriaAsSubtarefasRecebidasSobOPai() {
        Task pai = tarefa();
        when(repository.findById(pai.getId())).thenReturn(Optional.of(pai));
        List<TaskCommand> comandos = List.of(
                new TaskCommand("Sub 1", "Descricao 1", null, null),
                new TaskCommand("Sub 2", null, TaskPriority.HIGH, null));

        List<Task> criadas = service.apply(pai.getId(), comandos);

        assertThat(criadas).hasSize(2);
        assertThat(criadas).allSatisfy(criada -> assertThat(criada.getParent()).isSameAs(pai));
        assertThat(criadas.get(0).getTitle()).isEqualTo("Sub 1");
        assertThat(criadas.get(1).getPriority()).isEqualTo(TaskPriority.HIGH);
        verify(repository, times(2)).save(any(Task.class));
    }

    @Test
    void applyComIdInexistenteNaoCriaNenhumaSubtarefa() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        List<TaskCommand> comandos = List.of(new TaskCommand("Sub", null, null, null));

        assertThatThrownBy(() -> service.apply(id, comandos))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void applyPassaOsComandosNaOrdemRecebida() {
        Task pai = tarefa();
        when(repository.findById(pai.getId())).thenReturn(Optional.of(pai));
        List<TaskCommand> comandos = List.of(
                new TaskCommand("Primeira", null, null, null),
                new TaskCommand("Segunda", null, null, null),
                new TaskCommand("Terceira", null, null, null));

        service.apply(pai.getId(), comandos);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Task::getTitle)
                .containsExactly("Primeira", "Segunda", "Terceira");
    }

    @Test
    void applySuggestionNaRaizSemFilhasUsaHorizontalDaAnalise() {
        Task tarefa = tarefa();
        when(repository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(repository.findByParentIdOrderByCreatedAtAsc(tarefa.getId())).thenReturn(List.of());

        Task aplicada = service.applySuggestion(tarefa.getId(), TaskPriority.HIGH, 4.0);

        assertThat(aplicada.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(aplicada.getEstimatedTime()).isEqualTo(4.0);
        assertThat(aplicada.getEstimatedUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    void applySuggestionNaSubtarefaAplicaDiretoSemConsultarFilhas() {
        Task pai = tarefa();
        Task filha = new Task("Filha", null, TaskPriority.LOW, null, pai);
        when(repository.findById(filha.getId())).thenReturn(Optional.of(filha));

        service.applySuggestion(filha.getId(), TaskPriority.HIGH, 3.0);

        assertThat(filha.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(filha.getEstimatedTime()).isEqualTo(3.0);
        verify(repository, never()).findByParentIdOrderByCreatedAtAsc(any());
    }

    /** Porta fake: registra o contexto recebido e devolve o valor roteirizado. */
    private static final class IaFake implements TaskAiPort {

        private final List<TaskAiContext> contextos = new ArrayList<>();

        @Override
        public TaskImprovement improve(TaskAiContext context) {
            contextos.add(context);
            return new TaskImprovement("Titulo melhorado", "Descricao melhorada");
        }

        @Override
        public TaskAnalysis analyze(TaskAiContext context) {
            contextos.add(context);
            return new TaskAnalysis(TaskPriority.HIGH, TaskComplexity.MEDIUM, 12.0, "justificativa");
        }

        @Override
        public TaskDecomposition decompose(TaskAiContext context) {
            contextos.add(context);
            return new TaskDecomposition(List.of(
                    new ProposedSubtask("Subtitulo 1", "Subdescricao 1", 2.0),
                    new ProposedSubtask("Subtitulo 2", "Subdescricao 2", 3.0)));
        }
    }
}
