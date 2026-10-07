package com.desafio.taskmanager.ai.adapter;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.application.tools.TaskQueryTools;
import com.desafio.taskmanager.assistant.application.tools.dto.TaskToolResult;
import com.desafio.taskmanager.assistant.application.tools.dto.ToolResultPage;
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
 * modelo enxerga. As ferramentas de lista devolvem {@link ToolResultPage}
 * ({@code total} + {@code items}, T-F06-07): a descricao diz que o total e do
 * filtro inteiro para o modelo responder "3 de 40" em vez de truncar sem avisar.
 */
final class AssistantToolCallbacks {

    private final TaskQueryTools tools;

    AssistantToolCallbacks(TaskQueryTools tools) {
        this.tools = tools;
    }

    @Tool(name = "get_pending_tasks",
            description = "Lista as tarefas em aberto (TODO ou IN_PROGRESS) por urgencia: prazo mais proximo primeiro, sem prazo por ultimo, prioridade HIGH antes de MEDIUM e LOW. Devolve o total de tarefas em aberto e uma lista de itens.")
    public ToolResultPage get_pending_tasks() {
        return tools.getPendingTasks();
    }

    @Tool(name = "get_overdue_tasks",
            description = "Lista as tarefas com prazo vencido e ainda nao concluidas, do prazo mais antigo para o mais proximo. Devolve o total de tarefas atrasadas e uma lista de itens.")
    public ToolResultPage get_overdue_tasks() {
        return tools.getOverdueTasks();
    }

    @Tool(name = "get_task_by_id",
            description = "Busca uma tarefa pelo id; devolve vazio se o id nao existir.")
    public TaskToolResult get_task_by_id(
            @ToolParam(description = "id da tarefa (UUID)") UUID id) {
        return tools.getTaskById(id).orElse(null);
    }

    @Tool(name = "get_tasks_by_priority",
            description = "Lista as tarefas de uma prioridade (LOW, MEDIUM ou HIGH), qualquer status, por prazo mais proximo primeiro e sem prazo por ultimo. Devolve o total daquela prioridade e uma lista de itens.")
    public ToolResultPage get_tasks_by_priority(
            @ToolParam(description = "prioridade: LOW, MEDIUM ou HIGH") TaskPriority priority) {
        return tools.getTasksByPriority(priority);
    }

    @Tool(name = "get_tasks_due_soon",
            description = "Lista as tarefas com prazo entre hoje e hoje+days dias, do prazo mais proximo para o mais distante. Devolve o total nessa janela e uma lista de itens.")
    public ToolResultPage get_tasks_due_soon(
            @ToolParam(description = "quantidade de dias a frente, entre 1 e 365") int days) {
        return tools.getTasksDueSoon(days);
    }

    @Tool(name = "get_task_summary",
            description = "Indicadores das tarefas: total, pendentes, em andamento, concluidas e de prioridade alta.")
    public TaskSummary get_task_summary() {
        return tools.getTaskSummary();
    }
}