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

    List<Task> findByPriorityAndStatusInOrderByCreatedAtDesc(
            TaskPriority priority, List<TaskStatus> statuses);

    List<Task> findByDueDateBetweenOrderByDueDateAsc(LocalDate from, LocalDate to);

    List<Task> findByDueDateLessThanAndStatusNotOrderByDueDateAsc(LocalDate date, TaskStatus status);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @Query("select count(t) from Task t")
    long countAll();

    @Query("select count(t) from Task t where t.status = :status")
    long countByStatusValue(@Param("status") TaskStatus status);

    @Query("select count(t) from Task t where t.priority = :priority")
    long countByPriorityValue(@Param("priority") TaskPriority priority);

    @Query("select count(t) from Task t where t.priority = :priority and t.status <> :concluida")
    long countByPriorityValueAndNotDone(@Param("priority") TaskPriority priority,
                                        @Param("concluida") TaskStatus concluida);

    @Query("select count(t) from Task t where t.parent is not null")
    long countSubtasks();
}