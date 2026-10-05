package com.desafio.taskmanager.task.application;

import java.util.UUID;

import com.desafio.taskmanager.task.api.dto.CreateTaskRequest;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.api.dto.UpdateTaskRequest;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;

import org.springframework.stereotype.Component;

/**
 * Conversao entre a entidade {@link Task} e o DTO da API.
 *
 * <p>Mapeamento explicito, campo a campo: e o unico lugar que conhece os dois
 * lados. Um {@code MapStruct} traria um requisito de build a mais e esconderia
 * o contrato, entao fica escrito a mao.
 *
 * <p>A entidade tem regra de transicao de status (DONE e terminal); o mapper nao
 * contorna isso. Status so muda por {@link Task#changeStatus}, chamado pelo
 * service.
 */
@Component
public class TaskMapper {

    /** Constroi a entidade a partir do POST. O status inicial fica na entidade. */
    public Task toDomain(CreateTaskRequest request) {
        return new Task(
                request.normalizedTitle(),
                request.description(),
                request.priority(),
                request.dueDate(),
                null);
    }

    /**
     * Aplica o PUT sobre a entidade existente. O pai nao se altera por edicao de
     * conteudo, e o status tem endpoint proprio.
     */
    public void updateDomain(Task task, UpdateTaskRequest request) {
        task.updateContent(
                request.normalizedTitle(),
                request.description(),
                request.priority() == null ? TaskPriority.DEFAULT : request.priority(),
                request.dueDate());
    }

    /** Cria uma subtarefa a partir de um DTO e do pai (decomposicao de IA, RF-14). */
    public Task toSubtask(CreateTaskRequest request, Task parent) {
        return new Task(
                request.normalizedTitle(),
                request.description(),
                request.priority(),
                request.dueDate(),
                parent);
    }

    /** Entidade para DTO. A referencia ao pai sai so como id. */
    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                parentIdOf(task),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    private static UUID parentIdOf(Task task) {
        Task parent = task.getParent();
        return parent == null ? null : parent.getId();
    }
}