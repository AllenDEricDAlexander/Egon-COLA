package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.config.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;
import top.egon.cola.platform.rbac3.admin.runtime.repository.outbox.TransactionalOutboxAuthorizationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum;

class OutboxTransactionRollbackIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void mapsTheStableRbac3EnvelopeAndReturnsTheExistingReceiptOnReplay() {
        AtomicReference<OutboxMessage> captured = new AtomicReference<>();
        TransactionalOutbox outbox = message -> {
            captured.set(message);
            return new OutboxReceipt("persisted-event", message.idempotencyKey(), false);
        };
        var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                outbox, Clock.fixed(NOW, ZoneOffset.UTC));

        String messageId = adapter.enqueue(event());

        assertThat(messageId).isEqualTo("persisted-event");
        assertThat(captured.get().channel()).isEqualTo("rbac3-runtime");
        assertThat(captured.get().destination())
                .isEqualTo("rbac3.role-activation.changed.v1");
        assertThat(captured.get().idempotencyKey())
                .isEqualTo("7:rbac3.role-activation.changed.v1:99:4");
        assertThat(captured.get().payload().toString())
                .contains("eventId", "aggregateVersion=4")
                .doesNotContain("token", "password", "secret");
    }

    @Test
    void mapsSessionRevocationToTheStableRuntimeDestination() {
        AtomicReference<OutboxMessage> captured = new AtomicReference<>();
        var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                message -> {
                    captured.set(message);
                    return new OutboxReceipt(message.messageId(), message.idempotencyKey(), true);
                }, Clock.fixed(NOW, ZoneOffset.UTC));

        adapter.enqueue(new AuthorizationEventVO(
                "7", "SESSION", "99", "SESSION_REVOKED",
                Map.of("sessionVersion", "5", "reason", "ADMIN_REVOKE"),
                "session:99:5"));

        assertThat(captured.get().destination()).isEqualTo("rbac3.session.revoked.v1");
        assertThat(captured.get().idempotencyKey())
                .isEqualTo("7:rbac3.session.revoked.v1:99:5");
    }

    @Test
    void roleActivationUsesTheChangingContextVersionBeforeStaticPolicyVersions() {
        AtomicReference<OutboxMessage> captured = new AtomicReference<>();
        var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                message -> {
                    captured.set(message);
                    return new OutboxReceipt(
                            message.messageId(), message.idempotencyKey(), true);
                }, Clock.fixed(NOW, ZoneOffset.UTC));

        adapter.enqueue(new AuthorizationEventVO(
                "7", "SESSION", "99", "RBAC3_SESSION_ACTIVE_ROLES_REPLACED",
                Map.of(
                        "contextVersion", "6",
                        "authVersion", "1",
                        "policyVersion", "1"),
                "role-activation:99:6"));

        assertThat(captured.get().idempotencyKey())
                .isEqualTo("7:rbac3.role-activation.changed.v1:99:6");
    }

    @Test
    void deliveryRejectsUnknownDestinationsAndTreatsDuplicateProjectionAsSuccess()
            throws Exception {
        AtomicReference<String> applied = new AtomicReference<>();
        var handler = new Rbac3RuntimeProjectionDeliveryHandler(envelope -> {
            applied.set(envelope.eventId() + ':' + envelope.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.ALREADY_APPLIED;
        });

        assertThatThrownBy(() -> handler.validateDestination("rbac3.unknown.v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported RBAC3 runtime destination");
        DeliveryResult result = handler.deliver(context(
                "rbac3.role-activation.changed.v1",
                """
                        {"eventId":"event-1","eventType":"rbac3.role-activation.changed.v1",
                         "schemaVersion":1,"occurredAt":"2026-07-30T12:00:00Z",
                         "tenantId":"7","aggregateType":"SESSION","aggregateId":"99",
                         "aggregateVersion":4,"traceId":"trace-1","payload":{}}
                        """));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(applied.get()).isEqualTo("event-1:4");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RBAC3_IT_POSTGRES_URL", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "RBAC3_IT_POSTGRES_USER", matches = ".+")
    @EnabledIfEnvironmentVariable(
            named = "RBAC3_IT_POSTGRES_PASSWORD_FILE", matches = ".+")
    void twoFlywaysAndBusinessOutboxWriteShareOnePhysicalTransaction()
            throws Exception {
        String baseUrl = requiredEnvironment("RBAC3_IT_POSTGRES_URL");
        String user = requiredEnvironment("RBAC3_IT_POSTGRES_USER");
        String password = Files.readString(Path.of(requiredEnvironment(
                "RBAC3_IT_POSTGRES_PASSWORD_FILE"))).trim();
        String schema = "rbac3_it_" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16);
        try {
            execute(baseUrl, user, password, "create schema " + schema);
            String schemaUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                    + "currentSchema=" + schema;
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    schemaUrl, user, password);
            Flyway rbac3 = Rbac3FlywayConfiguration.buildRbac3Flyway(dataSource);
            Flyway outbox = Rbac3FlywayConfiguration.buildOutboxFlyway(dataSource);
            assertThat(rbac3.migrate().migrationsExecuted).isEqualTo(4);
            assertThat(outbox.migrate().migrationsExecuted).isEqualTo(1);

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("create table rbac3_it_business_tx (id bigint primary key)");
            DataSourceTransactionManager transactionManager =
                    new DataSourceTransactionManager(dataSource);
            try (AnnotationConfigApplicationContext context = outboxContext(
                    dataSource, transactionManager)) {
                TransactionalOutbox transactionalOutbox = context.getBean(
                        TransactionalOutbox.class);
                var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                        transactionalOutbox, Clock.fixed(NOW, ZoneOffset.UTC));
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);

                assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    jdbc.update("insert into rbac3_it_business_tx (id) values (1)");
                    adapter.enqueue(event());
                    throw new IllegalStateException("rollback requested");
                })).isInstanceOf(IllegalStateException.class);
                assertThat(count(jdbc, "rbac3_it_business_tx")).isZero();
                assertThat(count(jdbc, "egon_cola_outbox_message")).isZero();

                transaction.executeWithoutResult(status -> {
                    jdbc.update("insert into rbac3_it_business_tx (id) values (2)");
                    adapter.enqueue(event());
                });
                assertThat(count(jdbc, "rbac3_it_business_tx")).isEqualTo(1);
                assertThat(count(jdbc, "egon_cola_outbox_message")).isEqualTo(1);
                assertThat(count(jdbc, "flyway_schema_history_rbac3")).isPositive();
                assertThat(count(jdbc, "flyway_schema_history_outbox")).isPositive();
            }
        } finally {
            execute(baseUrl, user, password,
                    "drop schema if exists " + schema + " cascade");
        }
    }

    private AnnotationConfigApplicationContext outboxContext(
            DriverManagerDataSource dataSource,
            DataSourceTransactionManager transactionManager) {
        var context = new AnnotationConfigApplicationContext();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("egon.cola.component.transactional-outbox.enabled", "true");
        properties.put("egon.cola.component.transactional-outbox.node-id", "rbac3-it");
        properties.put("egon.cola.component.transactional-outbox.polling.enabled", "false");
        properties.put("egon.cola.component.transactional-outbox.storage.validate-schema", "true");
        properties.put("egon.cola.component.transactional-outbox.storage.data-source-bean-name", "dataSource");
        properties.put("egon.cola.component.transactional-outbox.storage.transaction-manager-bean-name", "transactionManager");
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("rbac3-outbox-it", properties));
        context.registerBean("dataSource", javax.sql.DataSource.class, () -> dataSource);
        context.registerBean("transactionManager",
                org.springframework.transaction.PlatformTransactionManager.class,
                () -> transactionManager);
        context.registerBean(ObjectMapper.class,
                () -> new ObjectMapper().findAndRegisterModules());
        context.registerBean(Rbac3RuntimeProjectionDeliveryHandler.class,
                () -> new Rbac3RuntimeProjectionDeliveryHandler(
                        envelope -> Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.APPLIED));
        context.register(TransactionalOutboxAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private long count(JdbcTemplate jdbc, String table) {
        Long value = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return value == null ? 0L : value;
    }

    private void execute(
            String url, String user, String password, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value.trim();
    }

    private AuthorizationEventVO event() {
        return new AuthorizationEventVO(
                "7", "SESSION", "99", "RBAC3_SESSION_ACTIVE_ROLES_REPLACED",
                Map.of("mutationId", "700", "sessionVersion", "4"), "trace-1");
    }

    private DeliveryContext context(String destination, String payload) {
        return new DeliveryContext(
                "message-1", "rbac3-runtime", destination, payload,
                "application/json", "1", Map.of(), "trace-1",
                1, 10, NOW.plusSeconds(30));
    }
}
