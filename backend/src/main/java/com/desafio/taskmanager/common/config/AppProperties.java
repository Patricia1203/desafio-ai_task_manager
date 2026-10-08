package com.desafio.taskmanager.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao geral de {@code app.*} compartilhada pelos adaptadores
 * (ver application.yml).
 *
 * @param timezone fuso do {@code java.time.Clock} usado como "agora" pelas
 *                 ferramentas do assistente e pela data do grounding
 *                 (APP_TIMEZONE; default America/Sao_Paulo)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone) {
}