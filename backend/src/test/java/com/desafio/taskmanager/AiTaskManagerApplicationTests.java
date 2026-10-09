package com.desafio.taskmanager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.desafio.taskmanager.support.PostgresIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-04: smoke de integracao. Sobe o contexto inteiro contra Postgres real via
 * Testcontainers e verifica que o schema das migrations foi aplicado.
 *
 * <p>O container vem de {@link PostgresIntegrationTest}. Esta classe nao escreve
 * em {@code tasks}, entao nao precisa limpar nada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AiTaskManagerApplicationTests extends PostgresIntegrationTest {

    /** {@code CREATE TABLE [IF NOT EXISTS] nome}, sem o ponto e virgula. */
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z0-9_]+)");

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

    @Test
    void todoNegocioVeioDoFlywayEAindaEstaNoHistorico() throws IOException {
        Set<String> criadasPelasMigrations = tabelasCriadasNasMigrations();

        // Guarda contra o teste passar por vazio: se o padrao de leitura das
        // migrations deixar de encontrar nada, o isSubsetOf abaixo aprova tudo.
        assertThat(criadasPelasMigrations).contains("tasks");

        Set<String> doBanco = new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class));

        // A lista vem do proprio Flyway em vez de uma constante no teste: quando a
        // F04 criar as tabelas de chat, este teste continua verde por ler a V3, e
        // continua vermelho se alguem criar tabela fora de migration.
        assertThat(doBanco)
                .as("tabelas do banco que nenhuma migration cria")
                .isSubsetOf(criadasPelasMigrations);
    }

    private Set<String> tabelasCriadasNasMigrations() throws IOException {
        Resource[] scripts = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/V*.sql");

        Set<String> tabelas = new HashSet<>();
        tabelas.add("flyway_schema_history");

        for (Resource script : scripts) {
            Matcher matcher = CREATE_TABLE.matcher(read(script));
            while (matcher.find()) {
                tabelas.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
        }

        return tabelas;
    }

    private String read(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}