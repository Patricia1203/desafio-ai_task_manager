package com.desafio.taskmanager.ai.adapter.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao de {@code app.ai.*} lida pelo adaptador (ver application.yml).
 *
 * <p>Sao os mesmos numeros que o {@code LlmResponseValidator} le por
 * {@code @Value} (T-F03-01): o validador e quem rejeita a resposta, o
 * adaptador e quem trunca o contexto e decide as variaveis do prompt, e os
 * dois precisam enxergar o mesmo limite.
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
