package com.desafio.taskmanager.task.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.desafio.taskmanager.task.api.dto.CreateTaskRequest;
import com.desafio.taskmanager.task.api.dto.PageResponse;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.api.dto.UpdateStatusRequest;
import com.desafio.taskmanager.task.api.dto.UpdateTaskRequest;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-01: o contrato entre entidade e DTO. Cobre round-trip completo, campos
 * opcionais e a validacao de entrada, que e o que o ProblemDetail vai mostrar
 * ao usuario.
 */
class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapper();

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** {@code Validator.validate} devolve {@code Set} e nao tem posicao fixa. */
    private static <T> ConstraintViolation<T> unica(Set<ConstraintViolation<T>> violations) {
        assertThat(violations).hasSize(1);
        return violations.iterator().next();
    }

    @Test
    void toDomainCriaTarefaComTodosOsCampos() {
        Task task = mapper.toDomain(new CreateTaskRequest(
                "  Escrever testes  ", "cobrir o service", TaskPriority.HIGH,
                LocalDate.of(2026, 11, 15)));

        assertThat(task.getTitle()).isEqualTo("Escrever testes");
        assertThat(task.getDescription()).isEqualTo("cobrir o service");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getParent()).isNull();
        assertThat(task.getId()).isNotNull();
    }

    @Test
    void toDomainAceitaCamposOpcionaisAusentes() {
        Task task = mapper.toDomain(new CreateTaskRequest("Minima", null, null, null));

        assertThat(task.getTitle()).isEqualTo("Minima");
        assertThat(task.getDescription()).isNull();
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getDueDate()).isNull();
    }

    @Test
    void toResponseCobreTodosOsCamposDaEntidade() {
        Task task = new Task("Titulo", "Descricao", TaskPriority.LOW, LocalDate.of(2026, 1, 1), null);

        TaskResponse response = mapper.toResponse(task);

        assertThat(response.id()).isEqualTo(task.getId());
        assertThat(response.title()).isEqualTo("Titulo");
        assertThat(response.description()).isEqualTo("Descricao");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.parentId()).isNull();
        assertThat(response.createdAt()).isEqualTo(task.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(task.getUpdatedAt());
    }

    @Test
    void toResponseExpoeParentIdDaSubtarefa() {
        Task parent = new Task("Pai", null, null, null, null);
        Task sub = mapper.toSubtask(new CreateTaskRequest("Filha", null, null, null), parent);

        TaskResponse response = mapper.toResponse(sub);

        assertThat(response.parentId()).isEqualTo(parent.getId());
        assertThat(response.title()).isEqualTo("Filha");
    }

    @Test
    void updateDomainAlteraConteudoEMantemStatusEPai() {
        Task parent = new Task("Pai", null, null, null, null);
        Task task = mapper.toSubtask(new CreateTaskRequest("Original", "antes", TaskPriority.LOW, null), parent);
        task.changeStatus(TaskStatus.IN_PROGRESS);
        Instant createdAt = task.getCreatedAt();

        mapper.updateDomain(task, new UpdateTaskRequest(
                "  Editada  ", "depois", TaskPriority.HIGH, LocalDate.of(2026, 3, 3)));

        assertThat(task.getTitle()).isEqualTo("Editada");
        assertThat(task.getDescription()).isEqualTo("depois");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 3));
        // o PUT nao mexe em status nem em parent
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getParent()).isSameAs(parent);
        assertThat(task.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void updateDomainLimpaCamposOpcionaisQuandoOmitidos() {
        Task task = new Task("T", "descricao", TaskPriority.HIGH, LocalDate.of(2026, 5, 5), null);

        mapper.updateDomain(task, new UpdateTaskRequest("T2", null, null, null));

        assertThat(task.getDescription()).isNull();
        assertThat(task.getDueDate()).isNull();
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void roundTripPreservaTodosOsCampos() {
        CreateTaskRequest original = new CreateTaskRequest(
                "Round trip", "texto", TaskPriority.HIGH, LocalDate.of(2026, 7, 7));

        TaskResponse response = mapper.toResponse(mapper.toDomain(original));
        TaskResponse volta = mapper.toResponse(mapper.toDomain(new CreateTaskRequest(
                response.title(), response.description(), response.priority(), response.dueDate())));

        assertThat(volta.title()).isEqualTo(response.title());
        assertThat(volta.description()).isEqualTo(response.description());
        assertThat(volta.priority()).isEqualTo(response.priority());
        assertThat(volta.dueDate()).isEqualTo(response.dueDate());
        assertThat(volta.status()).isEqualTo(response.status());
    }

    @Test
    void createRequestValidoNaoTemViolacao() {
        assertThat(VALIDATOR.validate(new CreateTaskRequest(
                "Valida", "descricao", TaskPriority.LOW, LocalDate.of(2026, 9, 9)))).isEmpty();
    }

    @Test
    void createRequestComTituloVazioApontaOCampo() {
        ConstraintViolation<CreateTaskRequest> violacao =
                unica(VALIDATOR.validate(new CreateTaskRequest("   ", null, null, null)));

        assertThat(violacao.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violacao.getMessage()).isEqualTo("titulo e obrigatorio");
    }

    @Test
    void createRequestComTituloLongoApontaOCampoComMensagem() {
        ConstraintViolation<CreateTaskRequest> violacao =
                unica(VALIDATOR.validate(new CreateTaskRequest("a".repeat(201), null, null, null)));

        assertThat(violacao.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violacao.getMessage()).isEqualTo("titulo deve ter no maximo 200 caracteres");
    }

    @Test
    void createRequestComDescricaoLongaApontaOCampo() {
        ConstraintViolation<CreateTaskRequest> violacao =
                unica(VALIDATOR.validate(new CreateTaskRequest("ok", "d".repeat(5001), null, null)));

        assertThat(violacao.getPropertyPath().toString()).isEqualTo("description");
        assertThat(violacao.getMessage()).isEqualTo("descricao deve ter no maximo 5000 caracteres");
    }

    @Test
    void updateRequestExigeTitulo() {
        ConstraintViolation<UpdateTaskRequest> violacao =
                unica(VALIDATOR.validate(new UpdateTaskRequest("", null, null, null)));

        assertThat(violacao.getPropertyPath().toString()).isEqualTo("title");
    }

    @Test
    void updateStatusRequestExigeStatus() {
        ConstraintViolation<UpdateStatusRequest> violacao =
                unica(VALIDATOR.validate(new UpdateStatusRequest(null)));

        assertThat(violacao.getPropertyPath().toString()).isEqualTo("status");
        assertThat(violacao.getMessage()).isEqualTo("status e obrigatorio");
    }

    @Test
    void updateStatusRequestAceitaOsTresStatus() {
        for (TaskStatus status : TaskStatus.values()) {
            assertThat(VALIDATOR.validate(new UpdateStatusRequest(status))).isEmpty();
        }
    }

    @Test
    void pageResponseMapeiaDoPageDoSpringData() {
        TaskResponse a = mapper.toResponse(new Task("A", null, null, null, null));
        TaskResponse b = mapper.toResponse(new Task("B", null, null, null, null));

        PageResponse<TaskResponse> response = PageResponse.of(new PageImpl<>(
                List.of(a, b), PageRequest.of(1, 2), 5));

        assertThat(response.content()).containsExactly(a, b);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void pageResponseDaPrimeiraPaginaMarcaFirst() {
        TaskResponse a = mapper.toResponse(new Task("A", null, null, null, null));

        PageResponse<TaskResponse> response = PageResponse.of(new PageImpl<>(
                List.of(a), PageRequest.of(0, 10), 1));

        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void idsSaoUnicosPorTarefa() {
        UUID primeiro = new Task("A", null, null, null, null).getId();
        UUID segundo = new Task("B", null, null, null, null).getId();

        assertThat(primeiro).isNotNull().isNotEqualTo(segundo);
    }
}