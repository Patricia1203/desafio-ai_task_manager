package com.desafio.taskmanager.task.infra;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso a tabela tasks. Filtros compostos e paginacao entram pelo
 * {@code JpaSpecificationExecutor}; as consultas de summary e as ferramentas
 * somente-leitura do assistente (F04) sao @Query nomeadas para ficarem
 * legiveis e reviewaveis.
 */
public interface TaskRepository
        extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndParentIsNull(UUID id);

    List<Task> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    List<Task> findByStatusOrderByCreatedAtDesc(TaskStatus status);

    /**
     * Vínculo em lote das tarefas sem quadro ao primeiro quadro criado (pedido do
     * usuário na F14: as tarefas já existentes passam a pertencer ao 1º quadro).
     * O {@code @Modifying} precisa de {@code clearAutomatically} ou ({@code flush})
     * porque o Service delega a transação e a lista consultada antes ficaria em
     * cache no persistence context; aqui o UPDATE roda direto no banco, então é
     * seguro após um flush.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Task t set t.area = :area where t.area is null")
    int assignAreaToOrphans(@Param("area") WorkArea area);

    /**
     * Tarefas em aberto ordenadas por urgencia (T-F06-07): prazo proximo primeiro,
     * sem prazo por ultimo, e dentro do mesmo prazo a prioridade mais alta. O
     * {@code createdAt desc} e o desempate para a ordem ser estavel.
     *
     * <p>A prioridade vem como parametro porque o JPQL nao tem literal de enum
     * portavel; {@code :high} e {@code :media} sao os dois primeiros patamares do
     * case e o resto cai no ultimo.
     */
    @Query("""
            select t from Task t
            where t.status in :statuses
            order by t.dueDate asc nulls last,
                     case when t.priority = :high then 0
                          when t.priority = :media then 1
                          else 2 end,
                     t.createdAt desc
            """)
    List<Task> findEmAbertoPorUrgencia(
            @Param("statuses") List<TaskStatus> statuses,
            @Param("high") TaskPriority high,
            @Param("media") TaskPriority media,
            Pageable pageable);

    /**
     * Tarefas de uma prioridade por urgencia (T-F06-07). Mesmo criterio do
     * {@code findEmAbertoPorUrgencia} — prazo, prioridade e recencia — porque o
     * design da F06 manda as duas ferramentas ordenarem pelas tres chaves.
     * Aqui o criterio de prioridade fica constante (a query filtra por ele), o
     * que e inofensivo e mantem as duas queries com a mesma forma.
     */
    @Query("""
            select t from Task t
            where t.priority = :priority
            order by t.dueDate asc nulls last,
                     case when t.priority = :high then 0
                          when t.priority = :media then 1
                          else 2 end,
                     t.createdAt desc
            """)
    List<Task> findPorPrioridadePorUrgencia(
            @Param("priority") TaskPriority priority,
            @Param("high") TaskPriority high,
            @Param("media") TaskPriority media,
            Pageable pageable);

    long countByStatusIn(List<TaskStatus> statuses);

    long countByDueDateBetween(LocalDate from, LocalDate to);

    long countByDueDateLessThanAndStatusNot(LocalDate date, TaskStatus status);

    List<Task> findByDueDateBetweenOrderByDueDateAsc(LocalDate from, LocalDate to, Pageable pageable);

    List<Task> findByDueDateLessThanAndStatusNotOrderByDueDateAsc(
            LocalDate date, TaskStatus status, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @Query("select count(t) from Task t where t.priority = :priority")
    long countByPriorityValue(@Param("priority") TaskPriority priority);

    /**
     * Itens finais do dashboard: uma tarefa que tem subtarefas nao conta, contam
     * as de menor nivel. Assim o KPI reflete "o que falta trabalhar" em vez de
     * contar pai e filhas como linhas independentes.
     */
    @Query("""
            select count(t) from Task t
            where not exists (select 1 from Task s where s.parent = t)
            """)
    long countLeaves();

    /**
     * Itens finais por status (T-F11-XX). Mesmo criterio do {@link #countLeaves()},
     * somado ao status informado.
     */
    @Query("""
            select count(t) from Task t
            where not exists (select 1 from Task s where s.parent = t) and t.status = :status
            """)
    long countLeavesByStatusValue(@Param("status") TaskStatus status);

    /**
     * Itens finais de uma prioridade ainda nao concluidos (T-F11-XX). O KPI de
     * alta prioridade conta o que resta fazer, nao o historico todo.
     */
    @Query("""
            select count(t) from Task t
            where not exists (select 1 from Task s where s.parent = t)
              and t.priority = :priority and t.status <> :done
            """)
    long countLeavesByPriorityValueAndNotDone(@Param("priority") TaskPriority priority,
                                              @Param("done") TaskStatus done);

    @Query("select count(t) from Task t where t.parent is not null")
    long countSubtasks();

    /**
     * Subtarefas por raiz, numa query so (T-F07-02): evita o N+1 da listagem.
     * Linha por rai z — {pai id, contagem}.
     */
    @Query("""
            select t.parent.id, count(t)
            from Task t
            where t.parent.id in :parentIds
            group by t.parent.id
            """)
    List<Object[]> subtaskCountsByRoot(@Param("parentIds") Collection<UUID> parentIds);
}