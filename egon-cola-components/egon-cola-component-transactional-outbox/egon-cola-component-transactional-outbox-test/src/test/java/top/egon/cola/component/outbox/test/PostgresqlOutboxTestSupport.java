package top.egon.cola.component.outbox.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.util.UUID;

abstract class PostgresqlOutboxTestSupport {

    private static final String TEST_SCHEMA =
            "egon_outbox_test_" + UUID.randomUUID().toString().replace("-", "");
    static PGSimpleDataSource dataSource;
    static JdbcTemplate jdbcTemplate;
    static DataSourceTransactionManager transactionManager;
    static ObjectMapper objectMapper;
    private static JdbcTemplate administrativeJdbcTemplate;

    @BeforeAll
    static void initializePostgresql() throws Exception {
        PGSimpleDataSource administrativeDataSource = localDataSource();
        administrativeJdbcTemplate = new JdbcTemplate(administrativeDataSource);
        administrativeJdbcTemplate.execute("create schema " + TEST_SCHEMA);

        dataSource = localDataSource();
        dataSource.setCurrentSchema(TEST_SCHEMA);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        objectMapper = new ObjectMapper();

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(
                    "db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql"));
        }
    }

    @AfterAll
    static void removeTestSchema() {
        if (administrativeJdbcTemplate != null) {
            administrativeJdbcTemplate.execute("drop schema if exists " + TEST_SCHEMA + " cascade");
        }
    }

    @BeforeEach
    void cleanOutboxTable() {
        jdbcTemplate.execute("truncate table egon_cola_outbox_message restart identity");
    }

    private static PGSimpleDataSource localDataSource() {
        String host = property("egon.outbox.test.postgresql.host", "EGON_OUTBOX_TEST_POSTGRES_HOST",
                "127.0.0.1");
        int port = Integer.parseInt(property(
                "egon.outbox.test.postgresql.port",
                "EGON_OUTBOX_TEST_POSTGRES_PORT",
                "5432"
        ));
        String database = property(
                "egon.outbox.test.postgresql.database",
                "EGON_OUTBOX_TEST_POSTGRES_DATABASE",
                "postgres"
        );
        String user = property(
                "egon.outbox.test.postgresql.user",
                "EGON_OUTBOX_TEST_POSTGRES_USER",
                "postgres"
        );
        String password = property(
                "egon.outbox.test.postgresql.password",
                "EGON_OUTBOX_TEST_POSTGRES_PASSWORD",
                System.getenv().getOrDefault("PGPASSWORD", "")
        );

        PGSimpleDataSource localDataSource = new PGSimpleDataSource();
        localDataSource.setServerNames(new String[]{host});
        localDataSource.setPortNumbers(new int[]{port});
        localDataSource.setDatabaseName(database);
        localDataSource.setUser(user);
        localDataSource.setPassword(password);
        return localDataSource;
    }

    private static String property(String systemProperty, String environmentVariable, String fallback) {
        String configured = System.getProperty(systemProperty);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        configured = System.getenv(environmentVariable);
        return configured == null || configured.isBlank() ? fallback : configured;
    }
}
