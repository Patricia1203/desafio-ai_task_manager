package com.desafio.taskmanager.ai.port;

import com.desafio.taskmanager.ai.port.dto.TaskAiContext;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;

/**
 * Porta de saída para as sugestões de IA sobre tarefas (RF-10 melhorar,
 * RF-11 analisar, RF-12 decompor). A camada de aplicação fala com esta
 * interface; quem importa Spring AI é só o adaptador em {@code ai.adapter}
 * (RNF-14): trocar o provedor tem que caber em adaptador + config, sem
 * Spring AI em {@code task.domain}/{@code task.application}.
 *
 * <p>Toda saída é record tipado, já passando pelo {@code LlmResponseValidator}
 * (RNF-10): o resto da aplicação nunca vê JSON cru do modelo.
 */
public interface TaskAiPort {

    /** Sugere título e descrição melhores, sem persistir nada (RF-10). */
    TaskImprovement improve(TaskAiContext context);

    /** Sugere prioridade, complexidade e horas estimadas, sem alterar a tarefa (RF-11). */
    TaskAnalysis analyze(TaskAiContext context);

    /** Propõe subtarefas sem criá-las: criar é decisão da aplicação (RF-12, RF-14). */
    TaskDecomposition decompose(TaskAiContext context);
}
