package top.egon.cola.component.outbox.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;

import java.sql.Connection;
import java.time.Instant;
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
    static synchronized void initializePostgresql() throws Exception {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv(
                        "EGON_OUTBOX_TEST_POSTGRES_ENABLED"
                )),
                "Set EGON_OUTBOX_TEST_POSTGRES_ENABLED=true "
                        + "to run PostgreSQL integration tests"
        );
        PostgreSQLContainer<?> postgresql = postgresql();
        if (!postgresql.isRunning()) {
            postgresql.start();
        }
        PGSimpleDataSource administrativeDataSource = containerDataSource();
        administrativeJdbcTemplate = new JdbcTemplate(administrativeDataSource);
        administrativeJdbcTemplate.execute("create schema " + TEST_SCHEMA);

        dataSource = containerDataSource();
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

    static PostgresqlJdbcOutboxStore outboxStore() {
        return new PostgresqlJdbcOutboxStore(
                jdbcTemplate,
                new NamedParameterJdbcTemplate(dataSource),
                objectMapper,
                transactionManager
        );
    }

    static NewOutboxRecord newRecord(String messageId) {
        return newRecord(messageId, "key-" + messageId, "a".repeat(64), 10);
    }

    static NewOutboxRecord newRecord(
            String messageId,
            String idempotencyKey,
            String fingerprint,
            int maxAttempts
    ) {
        return new NewOutboxRecord(
                messageId,
                idempotencyKey,
                fingerprint,
                "test",
                "orders",
                "{}",
                "application/json",
                "1",
                "{}",
                "trace-1",
                Instant.now().minusSeconds(1),
                maxAttempts
        );
    }

    private static PGSimpleDataSource containerDataSource() {
        PostgreSQLContainer<?> postgresql = postgresql();
        PGSimpleDataSource containerDataSource = new PGSimpleDataSource();
        containerDataSource.setURL(postgresql.getJdbcUrl());
        containerDataSource.setUser(postgresql.getUsername());
        containerDataSource.setPassword(postgresql.getPassword());
        return containerDataSource;
    }

    private static PostgreSQLContainer<?> postgresql() {
        return PostgresqlContainerHolder.INSTANCE;
    }

    private static final class PostgresqlContainerHolder {

        private static final PostgreSQLContainer<?> INSTANCE =
                new PostgreSQLContainer<>("postgres:16.6-alpine");

        private PostgresqlContainerHolder() {
        }
    }
}
