package com.desafio.taskmanager.common.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RNF-21: o CORS so aceita as origens de app.cors.allowed-origins.
 */
@WebMvcTest
@Import(WebConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173,http://localhost:8081")
class CorsConfigTest {

    private static final String ALLOWED = "http://localhost:5173";
    private static final String BLOCKED = "https://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    /** O HealthController depende de JdbcTemplate, que nao existe num slice web. */
    @TestConfiguration
    static class MockJdbcConfig {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }
    }

    @Test
    void preflightDeOrigemPermitidaERespondido() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED))
                // allowCredentials(false) faz o Spring omitir o header; o que nao pode
                // aparecer em resposta nenhuma e o valor "true".
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preflightDeOrigemNaoPermitidaNaoRecebeAllowOrigin() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, BLOCKED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void requisicaoDeOrigemNaoPermitidaNaoRecebeAllowOrigin() throws Exception {
        mockMvc.perform(get("/health").header(HttpHeaders.ORIGIN, BLOCKED))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void segundaOrigemConfiguradaEhLiberada() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"));
    }

    @Test
    void origemNaoPermitidaNaoEhEcoadaNoHeader() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173.evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}