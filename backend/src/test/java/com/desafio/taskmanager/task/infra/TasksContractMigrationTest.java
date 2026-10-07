package com.desafio.taskmanager.task.infra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.desafio.taskmanager.support.PostgresIntegrationTest;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TST-04 da migration V4 (T-F06-02): a conversao do contrato de tasks de
 * portugues para ingles precisa ser provada sobre uma base que realmente tem
 * dado em portugues — o resto da suite so ve a base ja migrada, onde a V4 nao
 * tem linha para converter.
 *
 * <p>O teste migra um schema isolado ate a V2, insere as tres tarefas com os
 * valores antigos, roda a V4 e confere o que sobrou: valor convertido, default
 * novo e CHECK recusando o valor de portugues. Schema proprio porque o container
 * e compartilhado e a V4 e irreversivel.
 */
@SpringBootTest
@ActiveProfiles("test")
class TasksContractMigrationTest extends PostgresIntegrationTest {

    private static final String SCHEMA = "contrato_v4";

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void recriaSchemaNaVersaoAntiga() throws SQLException {
        com.desafio.taskmanager.support.MigrationSchemas.drop(dataSource, SCHEMA);
        flyway().target("2").load().migrate();
    }

    @Test
    void converteOsValoresDePortuguesGravadosNaV2() throws SQLException {
        insereComValoresAntigos("Tarefa A", "A_FAZER", "BAIXA");
        insereComValoresAntigos("Tarefa B", "EM_ANDAMENTO", "MEDIA");
        insereComValoresAntigos("Tarefa C", "CONCLUIDA", "ALTA");

        // Antes da V4 o schema ainda e o de portugues: e isso que torna a
        // assercao seguinte prova de conversao, e nao de um insert qualquer.
        assertThat(status("Tarefa A")).isEqualTo("A_FAZER");
        assertThat(prioridade("Tarefa C")).isEqualTo("ALTA");

        flyway().target("4").load().migrate();

        assertThat(status("Tarefa A")).isEqualTo("TODO");
        assertThat(prioridade("Tarefa A")).isEqualTo("LOW");
        assertThat(status("Tarefa B")).isEqualTo("IN_PROGRESS");
        assertThat(prioridade("Tarefa B")).isEqualTo("MEDIUM");
        assertThat(status("Tarefa C")).isEqualTo("DONE");
        assertThat(prioridade("Tarefa C")).isEqualTo("HIGH");
    }

    @Test
    void deixaOsDefaultsEOCheckEmIngles() throws SQLException {
        flyway().target("4").load().migrate();

        assertThat(defaultDeStatus()).isEqualTo("TODO");
        assertThat(defaultDePrioridade()).isEqualTo("MEDIUM");
        assertThat(insereComValoresNovos("Valida", "TODO", "HIGH")).isTrue();
        assertThatThrownBy(() -> insereComValoresNovos("Invalida", "A_FAZER", "MEDIUM"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insereComValoresNovos("Invalida", "TODO", "CRITICA"))
                .isInstanceOf(SQLException.class);
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .defaultSchema(SCHEMA);
    }

    private void insereComValoresAntigos(String title, String status, String priority) throws SQLException {
        insereComValoresNovos(title, status, priority);
    }

    private boolean insereComValoresNovos(String title, String status, String priority) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".tasks (id, title, status, priority) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, title);
            statement.setString(3, status);
            statement.setString(4, priority);
            statement.execute();
            return true;
        }
    }

    private String status(String title) throws SQLException {
        return valorDaColuna(title, "status");
    }

    private String prioridade(String title) throws SQLException {
        return valorDaColuna(title, "priority");
    }

    private String valorDaColuna(String title, String coluna) throws SQLException {
        String sql = "SELECT " + coluna + " FROM " + SCHEMA + ".tasks WHERE title = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString(1);
            }
        }
    }

    private String defaultDeStatus() throws SQLException {
        return defaultDaColuna("status");
    }

    private String defaultDePrioridade() throws SQLException {
        return defaultDaColuna("priority");
    }

    private String defaultDaColuna(String coluna) throws SQLException {
        String sql = "SELECT column_default FROM information_schema.columns"
                + " WHERE table_schema = ? AND table_name = 'tasks' AND column_name = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SCHEMA);
            statement.setString(2, coluna);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                String raw = result.getString(1);
                return raw.replaceAll("^.*'(.*)'::.*$", "$1");
            }
        }
    }
}