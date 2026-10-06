package com.desafio.taskmanager.task.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.ai.port.TaskAiPort;
import com.desafio.taskmanager.ai.port.dto.TaskAiContext;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.domain.Task;
import com.desafio.taskmanager.task.domain.TaskPriority;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de IA sobre tarefas (F03): melhorar, analisar, decompor e
 * aplicar a decomposicao (RF-10 a RF-14).
 *
 * <p>As regras de negocio daqui sao as de "nunca por acidente":
 * <ul>
 *   <li>{@link #improve} e {@link #decompose} so devolvem sugestao — nenhuma
 *       escrita acontece; aplicar e decisao de quem chama;</li>
 *   <li>{@link #analyze} devolve a analise tipada e nao altera a tarefa
 *       (nem a prioridade) — a IA propoe, o sistema interpreta;</li>
 *   <li>{@link #apply} e o unico caminho de escrita: cria exatamente os
 *       comandos recebidos, cada subtarefa sob o pai do id do caminho, em uma
 *       transacao so (ou cria tudo, ou nada).</li>
 * </ul>
 *
 * <p>O service fala com {@link TaskAiPort} (a porta da feature) e com
 * {@link TaskService} (as regras de tarefa ja testadas); nao importa Spring
 * AI — quem conhece o provedor e so {@code ai.adapter} (RNF-14).
 */
@Service
@Transactional(readOnly = true)
public class AiTaskService {

    private final TaskAiPort ia;
    private final TaskService tarefas;

    public AiTaskService(TaskAiPort ia, TaskService tarefas) {
        this.ia = ia;
        this.tarefas = tarefas;
    }

    /**
     * RF-10. Sugestao de melhoria sem tocar no banco: a entrada e o proprio
     * conteudo a melhorar, sem id.
     *
     * <p>A prioridade do contexto e a default porque o pedido nao tem tarefa
     * associada e o prompt de melhoria so usa titulo e descricao — null aqui
     * seria contrato violado do {@link TaskAiContext}.
     */
    public TaskImprovement improve(String titulo, String descricao) {
        return ia.improve(new TaskAiContext(titulo, normaliza(descricao), TaskPriority.DEFAULT));
    }

    /** RF-11, RF-12. Analise da tarefa existente; id inexistente vira 404. */
    public TaskAnalysis analyze(UUID id) {
        return ia.analyze(contextoDe(tarefas.findById(id)));
    }

    /** RF-13. Sugestao de subtarefas sem criar nenhuma; id inexistente vira 404. */
    public TaskDecomposition decompose(UUID id) {
        return ia.decompose(contextoDe(tarefas.findById(id)));
    }

    /**
     * RF-14. Cria as subtarefas aceitas sob a tarefa do id.
     *
     * <p>A validacao dos drafts ja aconteceu na borda (Bean Validation do
     * request); aqui o que resta e 404 para pai inexistente e a escrita
     * atomica — o {@code @Transactional} garante que uma falha no meio nao
     * deixe meio caminho criado.
     */
    @Transactional
    public List<Task> apply(UUID id, List<TaskCommand> subtasks) {
        Task pai = tarefas.findById(id);
        List<Task> criadas = new ArrayList<>(subtasks.size());
        for (TaskCommand comando : subtasks) {
            criadas.add(tarefas.createSubtask(pai.getId(), comando));
        }
        return criadas;
    }

    private static TaskAiContext contextoDe(Task tarefa) {
        return new TaskAiContext(tarefa.getTitle(), normaliza(tarefa.getDescription()), tarefa.getPriority());
    }

    /** Descricao vazia e descricao nula sao a mesma materia-prima para o prompt. */
    private static String normaliza(String descricao) {
        return descricao == null ? "" : descricao;
    }
}
