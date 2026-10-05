package com.desafio.taskmanager.common.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check da aplicacao. Montado em /api/health pelo server.servlet.context-path.
 * Usa o healthcheck do docker-compose, entao nao depende de actuator.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> health() {
        String database = databaseStatus();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP".equals(database) ? "UP" : "DOWN");
        body.put("timestamp", Instant.now().toString());
        body.put("database", database);
        return body;
    }

    private String databaseStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (RuntimeException ex) {
            return "DOWN";
        }
    }
}