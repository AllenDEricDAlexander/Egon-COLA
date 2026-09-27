package top.egon.cola.component.outbox.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStore;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class PostgresqlOutboxTestSupport {

    static ConfigurableApplicationContext context;
    static DataSource dataSource;
    static JdbcTemplate jdbcTemplate;
    static JdbcTemplate physicalJdbcTemplate;
    static PlatformTransactionManager transactionManager;
    static ObjectMapper objectMapper;
    private static Path nativeRulesFile;

    @BeforeAll
    static synchronized void initializePostgresql() throws Exception {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv("EGON_OUTBOX_TEST_POSTGRES_ENABLED")),
                "Set EGON_OUTBOX_TEST_POSTGRES_ENABLED=true with explicit permission to run PostgreSQL integration tests"
        );
        PostgreSQLContainer<?> postgresql = postgresql();
        if (!postgresql.isRunning()) {
            postgresql.start();
        }
        new JdbcTemplate(containerDataSource(postgresql)).execute("CREATE SCHEMA IF NOT EXISTS egon_outbox");
        PGSimpleDataSource physicalDataSource = containerDataSource(postgresql);
        physicalDataSource.setCurrentSchema("egon_outbox");
        physicalJdbcTemplate = new JdbcTemplate(physicalDataSource);

        nativeRulesFile = Files.createTempFile("egon-outbox-test-native-rules-", ".yaml");
        Files.writeString(nativeRulesFile, """
                databaseName: egon
                rules:
                  - !SINGLE
                    tables:
                      - primary.egon_outbox.egon_cola_outbox_message
                      - primary.egon_outbox.ddl_history
                      - primary.public.outbox_test_order
                transaction:
                  defaultType: LOCAL
                """);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.application.name", "transactional-outbox-integration-test");
        properties.put("egon.cola.component.id.machine-id", "733");
        properties.put("egon.cola.component.id.max-clock-backward", "5ms");
        properties.put("egon.cola.component.mybatis-plus.sharding.enabled", "true");
        properties.put("egon.cola.component.mybatis-plus.sharding.mode", "SHARDING");
        properties.put("egon.cola.component.mybatis-plus.sharding.config-style", "NATIVE");
        properties.put("egon.cola.component.mybatis-plus.sharding.transaction-default-type", "LOCAL");
        properties.put("egon.cola.component.mybatis-plus.sharding.native-rules-resource", nativeRulesFile.toUri().toString());
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].name", "primary");
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].logical-name", "primary");
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].role", "PRIMARY");
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].driver-class-name", "org.postgresql.Driver");
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].jdbc-url", postgresql.getJdbcUrl());
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].username", postgresql.getUsername());
        properties.put("egon.cola.component.mybatis-plus.sharding.data-sources[0].password", postgresql.getPassword());
        properties.put("egon.cola.component.mybatis-plus.tenant-id.ignored-tables[0]", "egon_cola_outbox_message");
        properties.put("egon.cola.component.transactional-outbox.enabled", "true");
        properties.put("egon.cola.component.transactional-outbox.node-id", "outbox-test");
        properties.put("egon.cola.component.transactional-outbox.annotation.enabled", "false");
        properties.put("egon.cola.component.transactional-outbox.polling.enabled", "false");
        properties.put("egon.cola.component.transactional-outbox.storage.validate-schema", "true");
        context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run(
                        "--egon.cola.component.transactional-outbox.storage.mp.sql-session-factory-bean-name=sqlSessionFactory",
                        "--egon.cola.component.transactional-outbox.storage.mp.migration-mode=false",
                        "--egon.cola.component.transactional-outbox.storage.mp.migration-lock-timeout=30s",
                        "--egon.cola.component.transactional-outbox.storage.mp.manifest-resource=db/egon-outbox-mp/manifest.json"
                );
        dataSource = context.getBean(DataSource.class);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionManager = context.getBean(PlatformTransactionManager.class);
        objectMapper = context.getBean(ObjectMapper.class);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.outbox_test_order (
                    id bigint PRIMARY KEY,
                    state varchar(64) NOT NULL
                )
                """);
    }

    @AfterAll
    static synchronized void closePostgresqlContext() throws Exception {
        if (context != null) {
            context.close();
            context = null;
        }
        if (nativeRulesFile != null) {
            Files.deleteIfExists(nativeRulesFile);
            nativeRulesFile = null;
        }
        dataSource = null;
        jdbcTemplate = null;
        physicalJdbcTemplate = null;
        transactionManager = null;
        objectMapper = null;
    }

    @BeforeEach
    void cleanOutboxTables() {
        jdbcTemplate.execute("TRUNCATE TABLE egon_outbox.egon_cola_outbox_message");
        jdbcTemplate.execute("TRUNCATE TABLE public.outbox_test_order");
    }

    static OutboxStore outboxStore() {
        return context.getBean(OutboxStore.class);
    }

    static NewOutboxRecord newRecord(String messageId) {
        return newRecord(messageId, "key-" + messageId, "a".repeat(64), 10);
    }

    static NewOutboxRecord newRecord(String messageId, String idempotencyKey, String fingerprint, int maxAttempts) {
        return new NewOutboxRecord(messageId, idempotencyKey, fingerprint, "test", "orders", "{}",
                "application/json", "1", "{}", "trace-1", Instant.now(Clock.systemUTC()).minusSeconds(1),
                maxAttempts);
    }

    private static PGSimpleDataSource containerDataSource(PostgreSQLContainer<?> postgresql) {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(postgresql.getJdbcUrl());
        source.setUser(postgresql.getUsername());
        source.setPassword(postgresql.getPassword());
        return source;
    }

    private static PostgreSQLContainer<?> postgresql() {
        return PostgresqlContainerHolder.INSTANCE;
    }

    @org.springframework.boot.SpringBootConfiguration
    @EnableAutoConfiguration(excludeName =
            "top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheAutoConfiguration")
    static class TestApplication {
    }

    private static final class PostgresqlContainerHolder {

        private static final PostgreSQLContainer<?> INSTANCE =
                new PostgreSQLContainer<>("postgres:16.6-alpine");

        private PostgresqlContainerHolder() {
        }
    }
}
