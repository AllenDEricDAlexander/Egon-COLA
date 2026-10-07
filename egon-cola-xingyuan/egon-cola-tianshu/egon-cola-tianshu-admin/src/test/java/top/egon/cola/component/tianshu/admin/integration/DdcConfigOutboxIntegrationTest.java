package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.common.exception.OutboxTransactionMismatchException;
import top.egon.cola.component.outbox.transaction.OutboxTransactionGuard;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcConfigOutboxIntegrationTest {

    @Test
    void configTaskAndOutboxIntentUseOneDataSourceTransactionAndRollbackTogether() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires an explicitly configured isolated PostgreSQL/Outbox target");
        DataSource dataSource = dataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        OutboxTransactionGuard guard = new OutboxTransactionGuard(dataSource);
        Fixture fixture = new Fixture();

        transaction.executeWithoutResult(status -> {
            guard.requireSelectedTransaction();
            insertPublishTask(jdbc, fixture);
            insertOutboxMessage(jdbc, fixture);
            status.setRollbackOnly();
        });
        assertThat(count(jdbc, "SELECT count(*) FROM public.ddc_publish_task_t0 WHERE tenant_id = ? AND id = ?",
                fixture.tenantId(), fixture.taskId())).isZero();
        assertThat(count(jdbc, "SELECT count(*) FROM egon_outbox.egon_cola_outbox_message WHERE message_id = ?",
                fixture.messageId())).isZero();

        Fixture committed = new Fixture();
        transaction.executeWithoutResult(status -> {
            guard.requireSelectedTransaction();
            insertPublishTask(jdbc, committed);
            insertOutboxMessage(jdbc, committed);
        });
        assertThat(count(jdbc, "SELECT count(*) FROM public.ddc_publish_task_t0 WHERE tenant_id = ? AND id = ?",
                committed.tenantId(), committed.taskId())).isEqualTo(1);
        assertThat(count(jdbc, "SELECT count(*) FROM egon_outbox.egon_cola_outbox_message WHERE message_id = ?",
                committed.messageId())).isEqualTo(1);
    }

    @Test
    void outboxTransactionGuardRejectsASecondLogicalDataSource() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires an explicitly configured isolated PostgreSQL/Outbox target");
        DataSource selected = dataSource();
        DataSource other = dataSource();
        TransactionTemplate wrongTransaction = new TransactionTemplate(new DataSourceTransactionManager(other));
        OutboxTransactionGuard guard = new OutboxTransactionGuard(selected);

        assertThatThrownBy(() -> wrongTransaction.executeWithoutResult(status -> guard.requireSelectedTransaction()))
                .isInstanceOf(OutboxTransactionMismatchException.class);
    }

    @Test
    void workerAdvisoryClaimRetriesThenDeadLettersAnExhaustedMessage() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires an explicitly configured isolated PostgreSQL/Outbox target");
        DataSource dataSource = dataSource();
        Fixture fixture = new Fixture();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            insertOutboxMessage(connection, fixture);
            connection.commit();
        }

        Claim first = claimDue(dataSource, fixture.id(), "step24-worker");
        assertThat(first.attemptCount()).isEqualTo(1);
        assertThat(scheduleRetry(dataSource, fixture.id(), first.version(), "step24-worker")).isTrue();
        Claim second = claimDue(dataSource, fixture.id(), "step24-worker");
        assertThat(second.attemptCount()).isEqualTo(2);
        assertThat(markDead(dataSource, fixture.id(), second.version(), "step24-worker")).isTrue();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT status, attempt_count, last_error_code FROM egon_outbox.egon_cola_outbox_message "
                        + "WHERE id = ?")) {
            statement.setLong(1, fixture.id());
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("status")).isEqualTo("DEAD");
                assertThat(result.getInt("attempt_count")).isEqualTo(2);
                assertThat(result.getString("last_error_code")).isEqualTo("IT_RETRY_EXHAUSTED");
            }
        }
    }

    private static Claim claimDue(DataSource dataSource, long id, String owner) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            long version;
            String sourceStatus;
            try (PreparedStatement candidates = connection.prepareStatement(
                    "SELECT version, status FROM egon_outbox.egon_cola_outbox_message "
                            + "WHERE id = ? AND tenant_id = 0 AND deleted_at IS NULL "
                            + "AND ((status IN ('PENDING', 'RETRY_WAIT') AND next_attempt_at <= clock_timestamp()) "
                            + "OR (status = 'PROCESSING' AND locked_until < clock_timestamp())) "
                            + "ORDER BY next_attempt_at, id LIMIT 1")) {
                candidates.setLong(1, id);
                try (ResultSet result = candidates.executeQuery()) {
                    if (!result.next()) {
                        connection.commit();
                        return new Claim(0, -1);
                    }
                    version = result.getLong("version");
                    sourceStatus = result.getString("status");
                }
            }
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT pg_try_advisory_xact_lock(id) FROM egon_outbox.egon_cola_outbox_message "
                            + "WHERE id = ? AND tenant_id = 0 AND deleted_at IS NULL")) {
                lock.setLong(1, id);
                try (ResultSet result = lock.executeQuery()) {
                    if (!result.next() || !result.getBoolean(1)) {
                        connection.commit();
                        return new Claim(0, -1);
                    }
                }
            }
            String targetStatus = "PROCESSING";
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE egon_outbox.egon_cola_outbox_message SET status = ?, attempt_count = attempt_count + 1, "
                            + "locked_by = ?, locked_until = clock_timestamp() + interval '1 minute', "
                            + "updated_at = clock_timestamp(), update_user_id = 'system:outbox', "
                            + "update_time = clock_timestamp(), version = version + 1 "
                            + "WHERE id = ? AND version = ? AND status = ? AND tenant_id = 0 AND deleted_at IS NULL "
                            + "AND ((status IN ('PENDING', 'RETRY_WAIT') AND next_attempt_at <= clock_timestamp()) "
                            + "OR (status = 'PROCESSING' AND locked_until < clock_timestamp()))")) {
                update.setString(1, targetStatus);
                update.setString(2, owner);
                update.setLong(3, id);
                update.setLong(4, version);
                update.setString(5, sourceStatus);
                if (update.executeUpdate() != 1) {
                    connection.commit();
                    return new Claim(0, -1);
                }
            }
            try (PreparedStatement current = connection.prepareStatement(
                    "SELECT attempt_count, version FROM egon_outbox.egon_cola_outbox_message "
                            + "WHERE id = ? AND status = 'PROCESSING' AND locked_by = ? "
                            + "AND tenant_id = 0 AND deleted_at IS NULL FOR UPDATE")) {
                current.setLong(1, id);
                current.setString(2, owner);
                try (ResultSet result = current.executeQuery()) {
                    if (!result.next()) throw new IllegalStateException("claimed message could not be reloaded");
                    Claim claim = new Claim(result.getInt("attempt_count"), result.getLong("version"));
                    connection.commit();
                    return claim;
                }
            }
        }
    }

    private static boolean scheduleRetry(DataSource dataSource, long id, long expectedVersion, String owner)
            throws Exception {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE egon_outbox.egon_cola_outbox_message SET status = 'RETRY_WAIT', "
                        + "next_attempt_at = clock_timestamp() + (0 * interval '1 millisecond'), "
                        + "locked_by = NULL, locked_until = NULL, last_error_code = 'IT_RETRYABLE', "
                        + "last_error_message = 'retryable integration failure', completed_at = NULL, "
                        + "updated_at = clock_timestamp(), update_user_id = 'system:outbox', "
                        + "update_time = clock_timestamp(), version = version + 1 "
                        + "WHERE id = ? AND version = ? AND status = 'PROCESSING' AND locked_by = ? "
                        + "AND tenant_id = 0 AND deleted_at IS NULL")) {
            statement.setLong(1, id);
            statement.setLong(2, expectedVersion);
            statement.setString(3, owner);
            return statement.executeUpdate() == 1;
        }
    }

    private static boolean markDead(DataSource dataSource, long id, long expectedVersion, String owner)
            throws Exception {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE egon_outbox.egon_cola_outbox_message SET status = 'DEAD', locked_by = NULL, locked_until = NULL, "
                        + "last_error_code = 'IT_RETRY_EXHAUSTED', last_error_message = 'retry budget exhausted', "
                        + "completed_at = clock_timestamp(), updated_at = clock_timestamp(), "
                        + "update_user_id = 'system:outbox', update_time = clock_timestamp(), version = version + 1 "
                        + "WHERE id = ? AND version = ? AND status = 'PROCESSING' AND locked_by = ? "
                        + "AND tenant_id = 0 AND deleted_at IS NULL")) {
            statement.setLong(1, id);
            statement.setLong(2, expectedVersion);
            statement.setString(3, owner);
            return statement.executeUpdate() == 1;
        }
    }

    private record Claim(int attemptCount, long version) {
    }

    private static void insertPublishTask(JdbcTemplate jdbc, Fixture fixture) {
        jdbc.update("INSERT INTO public.ddc_publish_task_t0 (id, tenant_id, create_user_id, create_time, "
                        + "update_user_id, update_time, deleted_at, version, public_id, change_id, status, "
                        + "created_at, updated_at, attempt_count) VALUES (?, ?, 'step24-it', clock_timestamp(), "
                        + "'step24-it', clock_timestamp(), NULL, 0, ?, ?, 'PUBLISHING', clock_timestamp(), "
                        + "clock_timestamp(), 0)",
                fixture.taskId(), fixture.tenantId(), fixture.publicId(), fixture.changeId());
    }

    private static void insertOutboxMessage(JdbcTemplate jdbc, Fixture fixture) {
        jdbc.update("INSERT INTO egon_outbox.egon_cola_outbox_message (id, message_id, idempotency_key, "
                        + "message_fingerprint, channel, destination, payload, content_type, schema_version, "
                        + "headers_json, status, attempt_count, max_attempts, next_attempt_at, created_at, updated_at, "
                        + "tenant_id, create_user_id, create_time, update_user_id, update_time, deleted_at, version) "
                        + "VALUES (?, ?, ?, ?, 'tianshu.config-dispatch', ?, '{}', 'application/json', '1', '{}', "
                        + "'PENDING', 0, 2, clock_timestamp(), clock_timestamp(), clock_timestamp(), 0, "
                        + "'system:outbox', clock_timestamp(), 'system:outbox', clock_timestamp(), NULL, 0)",
                fixture.id(), fixture.messageId(), fixture.idempotencyKey(), "a".repeat(64), fixture.destination());
    }

    private static void insertOutboxMessage(Connection connection, Fixture fixture) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO egon_outbox.egon_cola_outbox_message (id, message_id, idempotency_key, "
                        + "message_fingerprint, channel, destination, payload, content_type, schema_version, "
                        + "headers_json, status, attempt_count, max_attempts, next_attempt_at, created_at, updated_at, "
                        + "tenant_id, create_user_id, create_time, update_user_id, update_time, deleted_at, version) "
                        + "VALUES (?, ?, ?, ?, 'tianshu.config-dispatch', ?, '{}', 'application/json', '1', '{}', "
                        + "'PENDING', 0, 2, clock_timestamp(), clock_timestamp(), clock_timestamp(), 0, "
                        + "'system:outbox', clock_timestamp(), 'system:outbox', clock_timestamp(), NULL, 0)")) {
            statement.setLong(1, fixture.id());
            statement.setString(2, fixture.messageId());
            statement.setString(3, fixture.idempotencyKey());
            statement.setString(4, "a".repeat(64));
            statement.setString(5, fixture.destination());
            statement.executeUpdate();
        }
    }

    private static long count(JdbcTemplate jdbc, String sql, Object... args) {
        Long count = jdbc.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    private static DataSource dataSource() {
        String url = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_JDBC_URL");
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_JDBC_URL must target PostgreSQL");
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_DB_USERNAME"));
        dataSource.setPassword(requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_DB_PASSWORD"));
        return dataSource;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for this IT");
        return value;
    }

    private record Fixture(long id, long tenantId, long taskId, String messageId,
                           String idempotencyKey, String destination, String publicId, String changeId) {
        private Fixture() {
            this(ThreadLocalRandom.current().nextLong(10_000_000_000_000L, Long.MAX_VALUE),
                    101L,
                    ThreadLocalRandom.current().nextLong(10_000_000_000_000L, Long.MAX_VALUE),
                    UUID.randomUUID().toString().replace("-", ""),
                    UUID.randomUUID().toString(),
                    "tianshu:v4:tenant:101:{" + "b".repeat(64) + "}:topic",
                    UUID.randomUUID().toString().replace("-", ""), UUID.randomUUID().toString().replace("-", ""));
        }
    }
}
