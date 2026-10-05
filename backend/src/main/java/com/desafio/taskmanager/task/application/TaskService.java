package com.desafio.taskmanager.task.application;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.api.dto.CreateTaskRequest;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.api.dto.UpdateTaskRequest;
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
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository repository;
    private final TaskMapper mapper;

    public TaskService(TaskRepository repository, TaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** RF-01. A tarefa nasce TODO; quem decide o status inicial e a entidade. */
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = mapper.toDomain(request);
        return mapper.toResponse(repository.save(task));
    }

    /** RF-03. Cria subtarefa sob um pai existente. Pai invalido vira 404. */
    @Transactional
    public TaskResponse createSubtask(UUID parentId, CreateTaskRequest request) {
        Task parent = getOrThrow(parentId);
        Task sub = mapper.toSubtask(request, parent);
        return mapper.toResponse(repository.save(sub));
    }

    /** RF-02. Filtros de status e prioridade, opcionalmente paginados. */
    public Page<TaskResponse> list(TaskFilter filter, Pageable pageable) {
        return repository.findAll(toSpecification(filter), pageable).map(mapper::toResponse);
    }

    /** Leitura sem paginacao, usada pelas ferramentas somente-leitura (F04). */
    public List<TaskResponse> findAll(TaskFilter filter) {
        return repository.findAll(toSpecification(filter)).stream().map(mapper::toResponse).toList();
    }

    /** RF-02. Id inexistente vira 404 pelo GlobalExceptionHandler. */
    public TaskResponse findById(UUID id) {
        return mapper.toResponse(getOrThrow(id));
    }

    /** RF-02. Subtarefas em ordem de criacao. */
    public List<TaskResponse> findSubtasks(UUID parentId) {
        getOrThrow(parentId);
        return repository.findByParentIdOrderByCreatedAtAsc(parentId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** RF-04. Nao mexe em status: a transicao tem o proprio endpoint. */
    @Transactional
    public TaskResponse update(UUID id, UpdateTaskRequest request) {
        Task task = getOrThrow(id);
        mapper.updateDomain(task, request);
        return mapper.toResponse(task);
    }

    /** RF-06. A regra de transicao e de dominio e vira 422 se for violada. */
    @Transactional
    public TaskResponse changeStatus(UUID id, TaskStatus newStatus) {
        Task task = getOrThrow(id);
        task.changeStatus(newStatus);
        return mapper.toResponse(task);
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

    private Task getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("tarefa", id));
    }

    /**
     * Monta a Specification a partir do filtro. Compondo com {@code and}, um
     * criterio nulo e simplesmente omitido, o que faz {@link TaskFilter#all()}
     * virar "todos".
     */
    private static Specification<Task> toSpecification(TaskFilter filter) {
        if (filter == null || filter.isEmpty()) {
            return Specification.unrestricted();
        }
        Specification<Task> spec = Specification.unrestricted();
        if (filter.hasStatus()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.hasPriority()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), filter.priority()));
        }
        return spec;
    }
}