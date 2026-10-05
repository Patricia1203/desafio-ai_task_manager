package com.desafio.taskmanager.common.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RNF-01: o health check reflete a disponibilidade real no codigo de resposta.
 *
 * <p><b>O que este teste protege.</b> O corpo ja dizia {@code DOWN} quando o
 * banco nao respondia, mas a resposta era 200 — e o healthcheck do Compose le
 * apenas o status HTTP. O Compose consideraria a aplicacao saudavel com o
 * Postgres fora do ar. Por isso DOWN precisa ser 503.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void comBancoNoArDevolve200ComStatusUp() throws Exception {
        given(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).willReturn(1);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void comBancoForaDevolve503ComStatusDown() throws Exception {
        willThrow(new DataAccessResourceFailureException(
                "conexao recusada em jdbc:postgresql://host:5432/taskmanager"))
                .given(jdbcTemplate).queryForObject(any(String.class), any(Class.class));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }

    @Test
    void comBancoForaNaoVazaDetalheDaFalha() throws Exception {
        willThrow(new DataAccessResourceFailureException(
                "conexao recusada em jdbc:postgresql://host:5432/taskmanager"))
                .given(jdbcTemplate).queryForObject(any(String.class), any(Class.class));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("jdbc:postgresql"))))
                .andExpect(content().string(not(containsString("DataAccessResourceFailureException"))))
                .andExpect(content().string(not(containsString("taskmanager"))));
    }
}