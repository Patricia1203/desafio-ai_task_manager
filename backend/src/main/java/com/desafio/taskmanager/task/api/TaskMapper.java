package com.desafio.taskmanager.task.api;

import java.util.Map;
import java.util.UUID;

import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.domain.Task;

import org.springframework.stereotype.Component;

/**
 * Conversao entre a entidade {@link Task} e o DTO da API.
 *
 * <p>Fica na camada da API de proposito (RNF-20): o service devolve a entidade e
 * quem conhece o formato de saida e o controller. Mapeamento explicito, campo a
 * campo — um {@code MapStruct} traria um requisito de build a mais e esconderia
 * o contrato, entao fica escrito a mao.
 *
 * <p>A entidade tem regra de transicao de status (DONE e terminal); o mapper
 * nao contorna isso. Status so muda por {@link Task#changeStatus}, chamado pelo
 * service.
 */
@Component
public class TaskMapper {

    /** Entidade para DTO. A referencia ao pai sai so como id. */
    public TaskResponse toResponse(Task task) {
        return toResponse(task, null);
    }

    /**
     * Variante usada na listagem (T-F07-02): {@code counts} traz as subtarefas de
     * cada raiz, calculadas numa unica query agrupada — e 0 quando a tarefa nao
     * tem vinculo ou o mapa nao foi informado.
     */
    public TaskResponse toResponse(Task task, Map<UUID, Long> counts) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getEstimatedTime(),
                task.getEstimatedUnit(),
                idTarefaPaiDe(task),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                counts == null ? 0L : counts.getOrDefault(task.getId(), 0L));
    }

    /**
     * O id do pai sai sem tocar no banco: {@code parent} e LAZY e o Hibernate
     * devolve o identificador do proxy sem inicializa-lo. Verificado por sonda que
     * mapeia fora de transacao, com {@code open-in-view: false} — se houvesse
     * SELECT, a sessao fechada estouraria {@code LazyInitializationException}.
     */
    private static UUID idTarefaPaiDe(Task task) {
        Task parent = task.getParent();
        return parent == null ? null : parent.getId();
    }
}
