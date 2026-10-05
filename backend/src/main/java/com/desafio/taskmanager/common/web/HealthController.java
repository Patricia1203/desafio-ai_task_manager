package com.desafio.taskmanager.common.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check da aplicacao. Montado em /api/health pelo server.servlet.context-path.
 * Usa o healthcheck do docker-compose, entao nao depende de actuator.
 *
 * <p><b>Por que o codigo de resposta importa aqui.</b> O healthcheck do Compose so
 * le o status HTTP: enquanto este metodo devolvesse 200, o Compose consideraria a
 * aplicacao saudavel com o Postgres fora do ar. O corpo ja dizia {@code DOWN}, mas
 * ninguem que automatizasse a leitura do corpo veria isso. Por isso o DOWN devolve
 * 503 (Service Unavailable) e o UP devolve 200.
 *
 * <p>A excecao do banco vai so para o log; a resposta expoe apenas UP/DOWN.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        String database = databaseStatus();
        boolean up = "UP".equals(database);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", up ? "UP" : "DOWN");
        body.put("timestamp", Instant.now().toString());
        body.put("database", database);

        return ResponseEntity
                .status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    private String databaseStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (RuntimeException ex) {
            log.warn("Health check falhou: o banco nao respondeu", ex);
            return "DOWN";
        }
    }
}