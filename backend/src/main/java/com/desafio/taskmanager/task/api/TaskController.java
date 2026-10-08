package com.desafio.taskmanager.task.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.desafio.taskmanager.task.api.dto.CreateTaskRequest;
import com.desafio.taskmanager.task.api.dto.PageResponse;
import com.desafio.taskmanager.task.api.dto.TaskResponse;
import com.desafio.taskmanager.task.api.dto.UpdateStatusRequest;
import com.desafio.taskmanager.task.api.dto.UpdateTaskRequest;
import com.desafio.taskmanager.task.application.TaskService;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.application.dto.TaskFilter;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;
import com.desafio.taskmanager.task.domain.TaskStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API de tarefas (RF-01 a RF-06, RF-20, RF-23).
 *
 * <p>O controller nao tem regra de negocio: valida a borda, delega ao service e
 * empacota a resposta. Erros sao convertidos em ProblemDetail pelo
 * {@code GlobalExceptionHandler}, entao nao ha try/catch aqui.
 *
 * <p>A ordem das rotas importa: {@code /tasks/summary} precisa vir antes de
 * {@code /tasks/{id}}, senao o Spring casa "summary" com o {id} e tenta
 * converter para UUID.
 *
 * <p><b>Sem {@code @Validated} de proposito.</b> A validacao de metodo nativa do
 * Spring 7 nos parametros do controller lanca {@code HandlerMethodValidationException},
 * que o handler global ja traduz em 400 com a lista de campos. Com {@code @Validated}
 * entra o proxy AOP antigo, que lanca {@code ConstraintViolationException} — nenhuma das
 * duas esta mapeada, e a violacao cai no 500 generico.
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    /** Evita que page * size derrube o banco com um size enorme. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Mais recentes primeiro, com o id desempatando createdAt. O filtro nao
     * expoe sort: a ordem e decisao de produto, nao parametro do cliente.
     */
    private static final Sort ORDENACAO_PADRAO = Sort.by(
            new Sort.Order(Sort.Direction.DESC, "createdAt"),
            new Sort.Order(Sort.Direction.ASC, "id"));

    private final TaskService service;
    private final TaskMapper mapper;

    public TaskController(TaskService service, TaskMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * RF-02, RF-23. Filtros e paginacao; ordenacao fixa em createdAt desc.
     *
     * <p><b>Por que o id entra como segundo criterio.</b> {@code createdAt} tem
     * granularidade de microssegundo, mas nao e unico: varias tarefas salvas no
     * mesmo instante recebem o mesmo timestamp. Nesses empates o Postgres pode
     * devolver as linhas em qualquer ordem entre consultas, e como a paginacao
     * usa OFFSET/LIMIT, a mesma tarefa pode aparecer em duas paginas ou sumir de
     * uma. O id como desempate torna o sort totalmente determinado: e unico e
     * nao muda depois de inserido.
     */
    @GetMapping
    public PageResponse<TaskResponse> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page nao pode ser negativo") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size deve ser no minimo 1")
            @Max(value = MAX_PAGE_SIZE, message = "size deve ser no maximo " + MAX_PAGE_SIZE) int size) {
        Page<Task> pagina = service.list(
                new TaskFilter(status, priority),
                PageRequest.of(page, size, ORDENACAO_PADRAO));
        Map<UUID, Long> subtaskCounts = service.subtaskCounts(
                pagina.getContent().stream().map(Task::getId).toList());
        return PageResponse.of(pagina.map(task -> mapper.toResponse(task, subtaskCounts)));
    }

    /** RF-20. Fica antes de /tasks/{id} por causa do casamento de rota. */
    @GetMapping("/summary")
    public TaskSummary summary() {
        return service.summary();
    }

    /** RF-02. Id inexistente vira 404 pelo service. */
    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable UUID id) {
        return mapper.toResponse(service.findById(id));
    }

    /** RF-02. Subtarefas em ordem de criacao; 404 se o pai nao existir. */
    @GetMapping("/{id}/subtasks")
    public List<TaskResponse> findSubtasks(@PathVariable UUID id) {
        return service.findSubtasks(id).stream().map(mapper::toResponse).toList();
    }

    /**
     * RF-01, ERR-02. 201 com Location apontando para a tarefa criada.
     *
     * <p><b>Por que {@code ServletUriComponentsBuilder} e nao uma string.</b> O
     * servico vive em {@code /api} por causa do {@code context-path}, e
     * {@code URI.create("/tasks/" + id)} ignoraria esse prefixo: o header
     * apontaria para um caminho que nao existe e o cliente receberia um 404 ao
     * seguir o proprio Location do 201. Montar a URI a partir da requisicao
     * corrente pega scheme, host, porta e context-path reais, o que tambem
     * funciona atras de proxy sem ajuste manual.
     */
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        Task created = service.create(comandoDe(request));
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(created));
    }

    /** RF-03. Substituicao de conteudo; o status tem endpoint proprio. */
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return mapper.toResponse(service.update(id, comandoDe(request)));
    }

    /** RF-06. Transicao de status; recusa vira 422, id inexistente vira 404. */
    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return mapper.toResponse(service.changeStatus(
                id, request.status(), Boolean.TRUE.equals(request.completeSubtasks())));
    }

    /**
     * RF-05. 204 sem corpo.
     *
     * <p>As subtarefas vao junto por cascata. A interface pede confirmacao
     * mostrando quais sao antes do DELETE — a tela busca
     * {@code GET /tasks/{id}/subtasks} e so entao confirma. O backend nao exige
     * confirmacao: o contrato do spec e "remove ou 404".
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Request da API para o comando neutro do application. A normalizacao do
     * titulo (trim) nao acontece aqui: o dominio faz isso ao construir a
     * entidade, entao o comando carrega o valor tal qual chegou.
     */
    private static TaskCommand comandoDe(CreateTaskRequest request) {
        return new TaskCommand(request.title(), request.description(), request.priority(), request.dueDate());
    }

    private static TaskCommand comandoDe(UpdateTaskRequest request) {
        return new TaskCommand(request.title(), request.description(), request.priority(), request.dueDate());
    }
}