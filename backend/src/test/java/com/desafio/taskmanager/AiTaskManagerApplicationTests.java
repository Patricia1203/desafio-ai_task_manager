package com.desafio.taskmanager;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-04: smoke de integracao. Sobe o contexto inteiro contra Postgres real via
 * Testcontainers e verifica que o schema das migrations foi aplicado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AiTaskManagerApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("taskmanager")
                    .withUsername("taskmanager")
                    .withPassword("taskmanager");

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextoCarregaComPostgresReal() {
        assertThat(context).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void flywayAplicouAMigracaoBase() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank");

        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).get("version")).isEqualTo("1");
        assertThat(rows.get(0).get("success")).isEqualTo(true);
    }

    @Test
    void nenhumaTabelaDeNegociCriadaPeloHibernate() {
        // ddl-auto=validate proibe o Hibernate de criar schema: a unica fonte e o Flyway.
        assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");

        List<String> tabelas = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        // O conjunto e fechado: as migrations V1..Vn sao a unica fonte do schema.
        // Se o Hibernate criasse algo, apareceria uma tabela fora desta lista.
        // (assumindo V1 sem DDL, V2 tasks, V3 chat na F04; na F03 e V1..V2)
        assertThat(tabelas).containsExactlyInAnyOrder("flyway_schema_history", "tasks");
    }
}