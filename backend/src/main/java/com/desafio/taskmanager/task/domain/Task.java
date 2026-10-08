package com.desafio.taskmanager.task.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.desafio.taskmanager.common.error.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tarefa. Entidade JPA, nunca exposta direto na API (ver CONVENTIONS.md):
 * a API usa os records de task.api.dto mapeados por TaskMapper.
 *
 * <p>Auto-relacionamento: {@code parent} e a tarefa pai quando esta linha e uma
 * subtarefa criada pela decomposicao de IA (RF-14). Mantido como referencia fraca
 * porque so a listagem de subtarefas precisa do pai.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** F12: tempo estimado para realizar (valor). Nulo = sem estimativa. */
    @Column(name = "estimated_time")
    private Double estimatedTime;

    /** F12: unidade do tempo estimado; nula junto com {@link #estimatedTime}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_unit", length = 10)
    private TimeUnit estimatedUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Task parent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Construtor para o JPA. Não use: toda criação passa pela factory. */
    protected Task() {
    }

    /**
     * Cria uma tarefa nova. O id e os timestamps sao gerados aqui para que a
     * entidade ja fique consistente antes de chegar ao banco.
     */
    public Task(String title, String description, TaskPriority priority, LocalDate dueDate, Task parent) {
        this(title, description, priority, dueDate, null, null, parent);
    }

    /**
     * Variante completa (F12): acrescenta o tempo estimado. Unidade sem valor
     * estimado e descartada e, valor sem unidade, vira HOURS como default.
     */
    public Task(String title, String description, TaskPriority priority, LocalDate dueDate,
            Double estimatedTime, TimeUnit estimatedUnit, Task parent) {
        this.id = UUID.randomUUID();
        this.title = requireTitle(title);
        this.description = description;
        this.status = TaskStatus.INITIAL;
        this.priority = priority == null ? TaskPriority.DEFAULT : priority;
        this.dueDate = dueDate;
        setEstimatedTime(estimatedTime, estimatedUnit);
        this.parent = parent;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;

        if (parent != null && parent.id != null && parent.id.equals(this.id)) {
            throw new BusinessRuleException("tarefa nao pode ser pai de si mesma");
        }
    }

/**
     * Altera o status respeitando o ciclo de vida.
     *
     * <p>Regra: DONE e terminal. Reabrir uma tarefa concluida exige voltar
     * explicitamente a TODO; ir direto para IN_PROGRESS a partir de
     * DONE e recusado porque perderia a informacao de que o trabalho ja foi
     * entregue.
     */
    public void changeStatus(TaskStatus newStatus) {
        Objects.requireNonNull(newStatus, "status nao pode ser nulo");
        if (this.status == newStatus) {
            return;
        }
        if (this.status == TaskStatus.DONE && newStatus != TaskStatus.TODO) {
            throw new BusinessRuleException("status nao pode ir de DONE para " + newStatus
                    + "; use TODO para reabrir");
        }
        this.status = newStatus;
        touch();
    }

    /** Edicao dos campos de conteudo. Status nao muda por aqui. */
    public void updateContent(String title, String description, TaskPriority priority, LocalDate dueDate) {
        this.title = requireTitle(title);
        this.description = description;
        this.priority = priority == null ? TaskPriority.DEFAULT : priority;
        this.dueDate = dueDate;
        touch();
    }

    /** F12: edicao do tempo estimado junto com as demais informacoes. */
    public void updateContent(String title, String description, TaskPriority priority, LocalDate dueDate,
            Double estimatedTime, TimeUnit estimatedUnit) {
        updateContent(title, description, priority, dueDate);
        setEstimatedTime(estimatedTime, estimatedUnit);
    }

    /**
     * F12: valor e unidade andam juntos. Valor sem unidade assume HOURS;
     * unidade sem valor e descartada. Valor informado deve ser positivo e
     * respeitar o teto da analise de IA (0, 200].
     */
    private void setEstimatedTime(Double estimatedTime, TimeUnit estimatedUnit) {
        if (estimatedTime == null) {
            this.estimatedTime = null;
            this.estimatedUnit = null;
            return;
        }
        if (estimatedTime <= 0 || estimatedTime > 200) {
            throw new BusinessRuleException("tempo estimado deve ser maior que zero e no maximo 200");
        }
        this.estimatedTime = estimatedTime;
        this.estimatedUnit = estimatedUnit == null ? TimeUnit.HOURS : estimatedUnit;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessRuleException("titulo nao pode ser vazio");
        }
        if (title.length() > 200) {
            throw new BusinessRuleException("titulo deve ter no maximo 200 caracteres");
        }
        return title.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Double getEstimatedTime() {
        return estimatedTime;
    }

    public TimeUnit getEstimatedUnit() {
        return estimatedUnit;
    }

    public Task getParent() {
        return parent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Verdadeiro quando a tarefa e uma subtarefa de outra. */
    public boolean isSubtask() {
        return parent != null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Task task) || id == null) {
            return false;
        }
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}