package com.desafio.taskmanager.support;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Drop de schema usado pelos testes de migration.
 *
 * <p>Existe porque o container de {@link PostgresIntegrationTest} e compartilhado
 * e a migration de contrato e irreversivel: o teste que prova a conversao de
 * portugues para ingles precisa comecar de novo na V2, num schema que nao tem
 * nada a ver com o da aplicacao.
 */
public final class MigrationSchemas {

    private MigrationSchemas() {
    }

    public static void drop(DataSource dataSource, String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}