package com.desafio.taskmanager.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relogio da aplicacao (F06, RF-16/RF-19): o {@code Clock} e o unico ponto de
 * "agora". Sem ele cada {@code LocalDate.now()} usaria o fuso JVM e o
 * assistente e as ferramentas divergiriam desteira os horarios gravados.
 *
 * <p>O fuso vem de {@code app.timezone} (default America/Sao_Paulo, o fuso dos
 * textos do dominio). Mockar o {@code Clock} nos testes - ou prender a mao com
 * um instante fixo - tambem faz o teste do prazo nao depender do dia em que a
 * suite roda.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ClockConfig {

    @Bean
    Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}