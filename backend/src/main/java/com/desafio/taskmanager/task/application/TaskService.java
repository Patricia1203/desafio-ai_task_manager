package com.desafio.taskmanager.task.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.application.dto.TaskFilter;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;
import com.desafio.taskmanager.task.infra.TaskRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio de tarefas (F02).
 *
 * <p>O service orquestra; as invariantes (titulo obrigatorio, DONE terminal)
 * ficam em {@link Task}. Aqui ficam so as regras que dependem de outras linhas
 * ou do filtro pedido.
 *
 * <p>RNF-20: a camada de aplicacao nao conhece a API. A entrada e o neutro
 * {@link TaskCommand} e a saida e a propria entidade — converter para
 * {@code TaskResponse} e papel do controller.
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /** RF-01. A tarefa nasce TODO; quem decide o status inicial e a entidade. */
    @Transactional
    public Task create(TaskCommand command) {
        return repository.save(toTask(command, null));
    }

    /** RF-14. Cria subtarefa sob um pai existente. Pai invalido vira 404. */
    @Transactional
    public Task createSubtask(UUID parentId, TaskCommand command) {
        Task parent = getOrThrow(parentId);
        return repository.save(toTask(command, parent));
    }

    /** RF-02. Filtros de status e prioridade, opcionalmente paginados. */
    public Page<Task> list(TaskFilter filter, Pageable pageable) {
        return repository.findAll(toSpecification(filter), pageable);
    }

    /** Leitura sem paginacao. Assim como {@link #list}, so raizes (T-F07-01). */
    public List<Task> findAll(TaskFilter filter) {
        return repository.findAll(toSpecification(filter));
    }

    /**
     * T-F07-02. Quantas subtarefas cada raiz tem, numa query agrupada so — a
     * listagem injeta isso na resposta sem disparar N+1.
     */
    public Map<UUID, Long> subtaskCounts(Collection<UUID> rootIds) {
        if (rootIds.isEmpty()) {
            return Map.of();
        }
        return repository.subtaskCountsByRoot(rootIds).stream().collect(Collectors.toMap(
                linha -> (UUID) linha[0],
                linha -> (Long) linha[1]));
    }

    /** RF-02. Id inexistente vira 404 pelo GlobalExceptionHandler. */
    public Task findById(UUID id) {
        return getOrThrow(id);
    }

    /** RF-02. Subtarefas em ordem de criacao. */
    public List<Task> findSubtasks(UUID parentId) {
        getOrThrow(parentId);
        return repository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    /** RF-04. Nao mexe em status: a transicao tem o proprio endpoint. */
    @Transactional
    public Task update(UUID id, TaskCommand command) {
        Task task = getOrThrow(id);
        task.updateContent(command.titulo(), command.descricao(), command.prioridade(), command.prazo());
        return task;
    }

    /** RF-06. A regra de transicao e de dominio e vira 422 se for violada. */
    @Transactional
    public Task changeStatus(UUID id, TaskStatus newStatus) {
        return changeStatus(id, newStatus, false);
    }

    /**
     * RF-06 + RF-14 (F09). Variante da conclusao em cascata.
     *
     * <p>Com {@code completeSubtasks == true} e {@code status == DONE}, a transicao do pai
     * conclui tambem as subtarefas diretas ainda nao concluidas, tudo na mesma transacao
     * (filhas ja {@code DONE} sao no-op). Filhas diretas apenas — o mesmo universo do modal
     * de exclusao em cascata e do bloco de subtarefas do detalhe. O campo e ignorado para
     * qualquer outro status; sem a flag o comportamento e identico ao de 2 argumentos.
     */
    @Transactional
    public Task changeStatus(UUID id, TaskStatus newStatus, boolean completeSubtasks) {
        Task task = getOrThrow(id);
        task.changeStatus(newStatus);
        if (completeSubtasks && newStatus == TaskStatus.DONE) {
            repository.findByParentIdOrderByCreatedAtAsc(id).stream()
                    .filter(subtask -> subtask.getStatus() != TaskStatus.DONE)
                    .forEach(subtask -> subtask.changeStatus(TaskStatus.DONE));
        }
        return task;
    }

    /**
     * RF-05. Exclui a tarefa e, por cascata do banco, as subtarefas.
     *
     * <p>A checagem de "existe" vem antes do {@code delete} para que um id
     * inexistente responda 404 em vez de 204 silencioso.
     */
    @Transactional
    public void delete(UUID id) {
        Task task = getOrThrow(id);
        repository.delete(task);
    }

    /** RF-20. Indicadores do dashboard, contados direto no banco. */
    public TaskSummary summary() {
        return new TaskSummary(
                repository.countAll(),
                repository.countByStatusValue(TaskStatus.TODO),
                repository.countByStatusValue(TaskStatus.IN_PROGRESS),
                repository.countByStatusValue(TaskStatus.DONE),
                repository.countByPriorityValue(TaskPriority.HIGH));
    }

    /** O status inicial e da entidade; o pai so existe na variante de subtarefa. */
    private static Task toTask(TaskCommand command, Task parent) {
        return new Task(
                command.titulo(),
                command.descricao(),
                command.prioridade(),
                command.prazo(),
                parent);
    }

    private Task getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("tarefa", id));
    }

    /**
     * Monta a Specification a partir do filtro. A lista publica e a de
     * tarefas-raiz (T-F07-01): a subtarefa nao aparece solta, vive no detalhe
     * do pai ({@code GET /tasks/{id}/subtasks}). Compondo com {@code and}, um
     * criterio nulo e simplesmente omitido — o que faz {@link TaskFilter#all()}
     * virar "toda raiz".
     */
    private static Specification<Task> toSpecification(TaskFilter filter) {
        Specification<Task> spec = raizSomente();
        if (filter != null && filter.hasStatus()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter != null && filter.hasPriority()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), filter.priority()));
        }
        return spec;
    }

    /** Raiz: {@code parent} nulo. Subtarefa so e alcancada pelo id ou pelo pai. */
    private static Specification<Task> raizSomente() {
        return (root, query, cb) -> cb.isNull(root.get("parent"));
    }
}
