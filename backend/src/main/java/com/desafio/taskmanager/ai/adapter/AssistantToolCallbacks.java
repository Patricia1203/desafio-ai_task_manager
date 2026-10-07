package com.desafio.taskmanager.ai.adapter;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.application.tools.TaskQueryTools;
import com.desafio.taskmanager.assistant.application.tools.dto.TaskToolResult;
import com.desafio.taskmanager.task.application.dto.TaskSummary;
import com.desafio.taskmanager.task.domain.TaskPriority;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * As ferramentas somente-leitura do assistente (US-032) expostas ao modelo
 * como tool calling.
 *
 * <p>Fica no adaptador para a anotacao do Spring AI nao vazar para
 * {@code assistant.application} (RNF-14, mesmo criterio do
 * {@code LayerDependenciesTest}): a classe so delega para o
 * {@link TaskQueryTools} testado em T-F04-02, e nenhum metodo escreve.
 *
 * <p>Os nomes em snake_case e as descricoes em portugues sao o contrato que o
 * modelo enxerga; o resultado de cada chamada e DTO enxuto serializado em JSON
 * pelo Spring AI.
 */
final class AssistantToolCallbacks {

    private final TaskQueryTools tools;

    AssistantToolCallbacks(TaskQueryTools tools) {
        this.tools = tools;
    }

    @Tool(name = "get_pending_tasks",
            description = "Lista as tarefas em aberto (a fazer ou em andamento), da mais recente para a mais antiga.")
    public List<TaskToolResult> get_pending_tasks() {
        return tools.getPendingTasks();
    }

    @Tool(name = "get_overdue_tasks",
            description = "Lista as tarefas com prazo vencido e ainda nao concluidas, do prazo mais antigo para o mais proximo.")
    public List<TaskToolResult> get_overdue_tasks() {
        return tools.getOverdueTasks();
    }

    @Tool(name = "get_task_by_id",
            description = "Busca uma tarefa pelo id; devolve vazio se o id nao existir.")
    public TaskToolResult get_task_by_id(
            @ToolParam(description = "id da tarefa (UUID)") UUID id) {
        return tools.getTaskById(id).orElse(null);
    }

    @Tool(name = "get_tasks_by_priority",
            description = "Lista as tarefas de uma prioridade (qualquer status), da mais recente para a mais antiga.")
    public List<TaskToolResult> get_tasks_by_priority(
            @ToolParam(description = "prioridade: LOW, MEDIUM ou HIGH") TaskPriority priority) {
        return tools.getTasksByPriority(priority);
    }

    @Tool(name = "get_tasks_due_soon",
            description = "Lista as tarefas com prazo entre hoje e hoje+days dias, do prazo mais proximo para o mais distante.")
    public List<TaskToolResult> get_tasks_due_soon(
            @ToolParam(description = "quantidade de dias a frente, entre 1 e 365") int days) {
        return tools.getTasksDueSoon(days);
    }

    @Tool(name = "get_task_summary",
            description = "Indicadores das tarefas: total, pendentes, em andamento, concluidas e de prioridade alta.")
    public TaskSummary get_task_summary() {
        return tools.getTaskSummary();
    }
}