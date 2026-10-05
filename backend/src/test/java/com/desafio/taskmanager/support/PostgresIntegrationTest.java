package com.desafio.taskmanager.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Postgres unico para toda a suite de integracao.
 *
 * <p><b>Por que um container so.</b> Com {@code @Container} em cada classe de
 * teste, o JUnit sobe e derruba um Postgres por classe: tres classes, tres
 * containers, tres boot de contexto. Como o container e identico em todos os
 * casos, o custo e multiplicado sem ganho. Aqui o container sobe uma vez no
 * class loader e as classes derivadas apontam para ele.
 *
 * <p><b>A troca e dado compartilhado.</b> Um unico banco significa que a tabela
 * {@code tasks} atravessa as classes de teste. Por isso as classes que escrevem
 * limpam no {@code @BeforeEach} — {@code TaskRepositoryTest} e
 * {@code TaskServiceTest} ja faziam isso. Quem escrever um teste de integracao
 * novo precisa limpar tambem, ou vai herdar linha de outro teste e ler um total
 * que nao pertence a ele.
 *
 * <p>Flyway roda uma vez por banco, o que e o comportamento correto: a migration
 * e a fonte do schema e nao faz sentido reaplicar por classe de teste.
 */
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = startOnce();

    private static PostgreSQLContainer startOnce() {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("taskmanager")
                .withUsername("taskmanager")
                .withPassword("taskmanager");
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        return container;
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}