package com.desafio.taskmanager.ai.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.ai.api.dto.AnalyzeTaskResponse;
import com.desafio.taskmanager.ai.api.dto.ApplyDecompositionRequest;
import com.desafio.taskmanager.ai.api.dto.DecomposeTaskResponse;
import com.desafio.taskmanager.ai.api.dto.ImproveTaskRequest;
import com.desafio.taskmanager.ai.api.dto.ImproveTaskResponse;
import com.desafio.taskmanager.ai.api.dto.SubtaskDraft;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.task.api.TaskMapper;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.application.AiTaskService;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TimeUnit;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API de IA sobre tarefas (US-020 a US-022): POST /improve, /{id}/analyze,
 * /{id}/decompose e /{id}/decompose/apply.
 *
 * <p>O controller valida a borda e delega ao {@link AiTaskService}, expondo
 * os records da porta em ingles (RNF-10) — o mesmo contrato dos outros DTOs da
 * api. Erros nao sao tratados aqui: {@code InvalidLlmResponseException},
 * {@code ResourceNotFoundException} e as violacoes de Bean Validation viram
 * ProblemDetail pelo {@code GlobalExceptionHandler}.
 *
 * <p>Sem {@code @Validated} de proposito (mesmo motivo do TaskController): a
 * validacao nativa do Spring 7 nos parametros ja cai no handler global como
 * 400.
 */
@RestController
@RequestMapping("/ai/tasks")
public class AiTaskController {

    private final AiTaskService service;
    private final TaskMapper mapper;

    public AiTaskController(AiTaskService service, TaskMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** RF-10. 200 com a sugestao; nada e persistido. */
    @PostMapping("/improve")
    public ImproveTaskResponse improve(@Valid @RequestBody ImproveTaskRequest request) {
        TaskImprovement sugestao = service.improve(request.title(), request.description());
        return new ImproveTaskResponse(sugestao.title(), sugestao.description());
    }

    /** RF-11, RF-12. 200 com a analise tipada; a tarefa nao muda. 404 se o id nao existe. */
    @PostMapping("/{id}/analyze")
    public AnalyzeTaskResponse analyze(@PathVariable UUID id) {
        TaskAnalysis analise = service.analyze(id);
        return new AnalyzeTaskResponse(
                analise.priority(), analise.complexity(), analise.estimatedHours(), analise.reason());
    }

    /** RF-13. 200 com as sugestoes; nenhuma subtarefa e criada aqui. */
    @PostMapping("/{id}/decompose")
    public DecomposeTaskResponse decompose(@PathVariable UUID id) {
        TaskDecomposition decomposicao = service.decompose(id);
        return new DecomposeTaskResponse(decomposicao.subtasks().stream()
                .map(proposta -> new DecomposeTaskResponse.SubtaskSuggestion(
                        proposta.title(), proposta.description(), proposta.estimatedHours()))
                .toList());
    }

    /**
     * RF-14. 201 com as subtarefas criadas e o Location apontando para a
     * lista delas sob a tarefa pai — o cliente segue o proprio header para
     * ver o resultado.
     *
     * <p>{@code fromCurrentContextPath} inclui o {@code context-path /api},
     * entao o Location certo sai mesmo atras do prefixo (mesma correcao de
     * T-F02-05c no POST /tasks).
     */
    @PostMapping("/{id}/decompose/apply")
    public ResponseEntity<List<TaskResponse>> apply(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyDecompositionRequest request) {
        List<TaskCommand> comandos = request.subtasks().stream()
                .map(AiTaskController::comandoDe)
                .toList();
        List<TaskResponse> criadas = service.apply(id, comandos).stream()
                .map(mapper::toResponse)
                .toList();
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/tasks/{id}/subtasks")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(criadas);
    }

    /**
     * A IA sempre propoe horas: o estimatedHours vira committedTime e HOURS.
     * F12 passou a persistir esse tempo (antes caia na validacao e no usuario, sem coluna).
     */
    private static TaskCommand comandoDe(SubtaskDraft draft) {
        return new TaskCommand(draft.title(), draft.description(), null, null,
                draft.estimatedHours(), TimeUnit.HOURS);
    }
}
