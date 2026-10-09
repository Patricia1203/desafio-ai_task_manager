package com.desafio.taskmanager.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao de CORS lida de app.cors.* (ver application.yml).
 *
 * @param allowedOrigins origens liberadas; lista vazia desabilita o CORS
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public boolean allowsOrigin(String origin) {
        return allowedOrigins.contains(origin);
    }
}