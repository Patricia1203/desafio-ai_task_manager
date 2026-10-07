package com.desafio.taskmanager.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites do assistente (F04) sob {@code app.assistant.*} (ver application.yml).
 *
 * <p>Record registrado por {@code @EnableConfigurationProperties} junto com
 * {@code AiProperties} (mesmo padrao do projeto). Record nao vira
 * {@code @Component}: no caminho de component scan o construtor seria satisfeito
 * por beans do container, nao por propriedades, quebrando o binding.
 *
 * @param maxToolResults limite de resultados de cada ferramenta somente-leitura
 * @param toolCalling true registra as ferramentas como tool calling; false injeta
 *     um contexto pre-montado no prompt (fallback para modelo que nao suporta)
 */
@ConfigurationProperties(prefix = "app.assistant")
public record AssistantLimitsProperties(int maxToolResults, boolean toolCalling) {
}