package com.desafio.taskmanager.ai.adapter.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao de {@code app.ai.*} lida pelo adaptador (ver application.yml).
 *
 * <p>Fonte única dos limites de {@code app.ai.*}: o {@code LlmResponseValidator}
 * e o adaptador recebem este record (T-F06-14), então o mesmo número vale na
 * validação da resposta e no truncamento do contexto/variáveis do prompt.
 *
 * @param timeout limite de conexao e de leitura no cliente do Ollama (AI_TIMEOUT)
 * @param maxRetries tentativas extras apos saida invalida (AI_MAX_RETRIES); a primeira chamada nao conta
 * @param minSubtasks menor quantidade de subtarefas aceita (variavel do task-decompose.st)
 * @param maxSubtasks maior quantidade de subtarefas aceita (variavel do task-decompose.st)
 * @param maxEstimatedHours teto de horas estimadas (variavel do task-analyze.st)
 * @param maxTitleLength limite de titulo no truncamento do contexto e na validacao da resposta
 * @param maxTextLength limite de descricao no truncamento do contexto e na validacao da resposta
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        Duration timeout,
        int maxRetries,
        int minSubtasks,
        int maxSubtasks,
        double maxEstimatedHours,
        int maxTitleLength,
        int maxTextLength) {
}
