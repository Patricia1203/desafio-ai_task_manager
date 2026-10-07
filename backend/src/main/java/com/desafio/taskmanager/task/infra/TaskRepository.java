package com.desafio.taskmanager.task.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
     * Tarefas de uma prioridade por urgencia. Aqui o criterio de prioridade e
     * constante (a query filtra por ele), entao sobra prazo e recencia como
     * ordem — o case de prioridade seria no-op.
     */
    @Query("""
            select t from Task t
            where t.priority = :priority
            order by t.dueDate asc nulls last, t.createdAt desc
            """)
    List<Task> findPorPrioridadePorUrgencia(
            @Param("priority") TaskPriority priority, Pageable pageable);

    long countByStatusIn(List<TaskStatus> statuses);

    long countByDueDateBetween(LocalDate from, LocalDate to);

    long countByDueDateLessThanAndStatusNot(LocalDate date, TaskStatus status);

    List<Task> findByDueDateBetweenOrderByDueDateAsc(LocalDate from, LocalDate to, Pageable pageable);

    List<Task> findByDueDateLessThanAndStatusNotOrderByDueDateAsc(
            LocalDate date, TaskStatus status, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @Query("select count(t) from Task t")
    long countAll();

    @Query("select count(t) from Task t where t.status = :status")
    long countByStatusValue(@Param("status") TaskStatus status);

    @Query("select count(t) from Task t where t.priority = :priority")
    long countByPriorityValue(@Param("priority") TaskPriority priority);

    @Query("select count(t) from Task t where t.priority = :priority and t.status <> :done")
    long countByPriorityValueAndNotDone(@Param("priority") TaskPriority priority,
                                        @Param("done") TaskStatus done);

    @Query("select count(t) from Task t where t.parent is not null")
    long countSubtasks();
}