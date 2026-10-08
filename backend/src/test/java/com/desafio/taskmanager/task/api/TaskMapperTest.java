package com.desafio.taskmanager.task.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * TST-01: o contrato de saida da API e a validacao de entrada. Cobre o
 * mapeamento entidade -> DTO, campos opcionais e as constraints que viram
 * ProblemDetail para o usuario.
 *
 * <p>A outra metade do antigo round-trip (DTO -> entidade) agora mora na
 * entidade: construcao, trim e prioridade default sao regras de dominio,
 * cobertas por {@code TaskTest}, e o fluxo completo por {@code TaskServiceTest}.
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
    void toResponseComCountsPreencheSubtaskCount() {
        Task raiz = new Task("Raiz", null, null, null, null);

        assertThat(mapper.toResponse(raiz, Map.of(raiz.getId(), 3L)).subtaskCount()).isEqualTo(3L);
        assertThat(mapper.toResponse(raiz, null).subtaskCount()).isZero();
        assertThat(mapper.toResponse(raiz).subtaskCount()).isZero();
    }

    @Test
    void toResponseExpoeParentIdDaSubtarefa() {
        Task parent = new Task("Pai", null, null, null, null);
        Task sub = new Task("Filha", null, null, null, parent);

        TaskResponse response = mapper.toResponse(sub);

        assertThat(response.parentId()).isEqualTo(parent.getId());
        assertThat(response.title()).isEqualTo("Filha");
    }

    @Test
    void toResponseExpoeTituloJaAparadoPeloDominio() {
        Task task = new Task("  Espacado  ", null, null, null, null);

        assertThat(mapper.toResponse(task).title()).isEqualTo("Espacado");
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
                unica(VALIDATOR.validate(new UpdateStatusRequest(null, null)));

        assertThat(violacao.getPropertyPath().toString()).isEqualTo("status");
        assertThat(violacao.getMessage()).isEqualTo("status e obrigatorio");
    }

    @Test
    void updateStatusRequestAceitaOsTresStatus() {
        for (TaskStatus status : TaskStatus.values()) {
            assertThat(VALIDATOR.validate(new UpdateStatusRequest(status, null))).isEmpty();
            assertThat(VALIDATOR.validate(new UpdateStatusRequest(status, true))).isEmpty();
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
}
