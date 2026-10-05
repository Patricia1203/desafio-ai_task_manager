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

        // contains, e nao containsExactlyInAnyOrder: este teste quer provar que as
        // tabelas do Flyway existem, nao que o schema tem exatamente N tabelas.
        // containsExactly falharia no dia em que a F04 criar as tabelas de chat,
        // por um motivo sem relacao com o que esta sendo verificado.
        assertThat(tabelas).contains("flyway_schema_history", "tasks");
    }

    /** Nenhuma tabela de negocio pode aparecer fora das migrations aplicadas. */
    @Test
    void todoNegocioVeioDoFlywayEAindaEstaNoHistorico() {
        List<String> tabelas = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tabelas).allSatisfy(tabela ->
                assertThat(tabela)
                        .as("tabela %s nao veio de migration", tabela)
                        .satisfiesAnyOf(
                                t -> assertThat(t).isEqualTo("flyway_schema_history"),
                                t -> assertThat(t).isIn("tasks")));
    }
}