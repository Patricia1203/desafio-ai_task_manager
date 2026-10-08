package com.desafio.taskmanager.assistant.application.tools;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ferramentas somente-leitura do assistente (F04, RF-19). Nenhum metodo escreve:
 * so le o repositorio e devolve um DTO enxuto, cortado em
 * {@code app.assistant.max-tool-results}.
 *
 * <p><b>As ferramentas de lista devolvem {@link ToolResultPage} (T-F06-07).</b>
 * Antes devolviam so a lista ja cortada e o modelo respondia "sao 3 pendentes"
 * sobre 40 existentes. Agora o total do filtro vem junto, e o corte acontece no
 * banco ({@code Pageable}), nao depois de materializar tudo em memoria.
 *
 * <p>RNF-13 nao depende de disciplina: a classe nao tem metodo de escrita e o
 * teste verifica que os metodos de escrita do repositorio nunca sao chamados.
 */
@Component
@Transactional(readOnly = true)
public class TaskQueryTools {

    /** Teto da janela de "vence em breve": nada alem de um ano. */
    private static final int MAX_DIAS_JANELA = 365;

    private static final List<TaskStatus> EM_ABERTO = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    private final TaskRepository repository;
    private final int maxToolResults;
    private final Clock clock;

    public TaskQueryTools(TaskRepository repository, AssistantLimitsProperties properties, Clock clock) {
        this.repository = repository;
        this.maxToolResults = properties.maxToolResults();
        this.clock = clock;
    }

    /** Tarefas em aberto (TODO ou IN_PROGRESS) por urgencia: prazo, prioridade e recencia. */
    public ToolResultPage getPendingTasks() {
        return pagina(
                repository.countByStatusIn(EM_ABERTO),
                repository.findEmAbertoPorUrgencia(EM_ABERTO, TaskPriority.HIGH, TaskPriority.MEDIUM, limite()));
    }

    /** Tarefas com prazo vencido e ainda nao concluidas, do prazo mais antigo para o mais proximo. */
    public ToolResultPage getOverdueTasks() {
        LocalDate hoje = LocalDate.now(clock);
        return pagina(
                repository.countByDueDateLessThanAndStatusNot(hoje, TaskStatus.DONE),
                repository.findByDueDateLessThanAndStatusNotOrderByDueDateAsc(hoje, TaskStatus.DONE, limite()));
    }

    /** Tarefa por id; id inexistente devolve vazio em vez de erro. */
    public Optional<TaskToolResult> getTaskById(UUID id) {
        return repository.findById(id).map(TaskToolResult::de);
    }

    /** Tarefas de uma prioridade (qualquer status), por prazo e recencia. */
    public ToolResultPage getTasksByPriority(TaskPriority priority) {
        Objects.requireNonNull(priority, "priority nao pode ser nula");
        return pagina(
                repository.countByPriorityValue(priority),
                repository.findPorPrioridadePorUrgencia(priority, TaskPriority.HIGH, TaskPriority.MEDIUM, limite()));
    }

    /** Tarefas com prazo entre hoje e hoje+days, do prazo mais proximo para o mais distante. */
    public ToolResultPage getTasksDueSoon(int days) {
        if (days < 1 || days > MAX_DIAS_JANELA) {
            throw new BusinessRuleException("days deve estar entre 1 e " + MAX_DIAS_JANELA);
        }
        LocalDate hoje = LocalDate.now(clock);
        LocalDate fim = hoje.plusDays(days);
        return pagina(
                repository.countByDueDateBetween(hoje, fim),
                repository.findByDueDateBetweenOrderByDueDateAsc(hoje, fim, limite()));
    }

    /**
     * Indicadores, mesmos numeros do dashboard (RF-20) para o assistente responder
     * com base: itens finais (tarefa com subtarefa nao conta) e alta prioridade
     * so com as ainda nao concluidas.
     */
    public TaskSummary getTaskSummary() {
        return new TaskSummary(
                repository.countLeaves(),
                repository.countLeavesByStatusValue(TaskStatus.TODO),
                repository.countLeavesByStatusValue(TaskStatus.IN_PROGRESS),
                repository.countLeavesByStatusValue(TaskStatus.DONE),
                repository.countLeavesByPriorityValueAndNotDone(TaskPriority.HIGH, TaskStatus.DONE));
    }

    private Pageable limite() {
        return PageRequest.of(0, maxToolResults);
    }

    /** O total e o do filtro inteiro; os itens sao o que coube no limite. */
    private static ToolResultPage pagina(long total, List<Task> tarefas) {
        return new ToolResultPage(total, tarefas.stream().map(TaskToolResult::de).toList());
    }
}