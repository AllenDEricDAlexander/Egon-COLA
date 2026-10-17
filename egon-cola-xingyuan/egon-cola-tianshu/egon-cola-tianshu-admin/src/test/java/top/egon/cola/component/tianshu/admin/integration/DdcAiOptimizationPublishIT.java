package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration;
import top.egon.cola.component.tianshu.admin.integration.DdcAiFeedbackPersistenceIT.Fixture;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TEST-015：自动发布的发布回执、EVENT-103 Outbox 行与运行终态在同一数据源事务内提交；入队失败回滚发布。
 * / TEST-015: the auto-publish receipt, its EVENT-103 Outbox row and the run outcome commit in one data-source
 * transaction; an enqueue failure rolls the publication back.
 */
class DdcAiOptimizationPublishIT {

    @Test
    void autoPublishRunProducesPublicationReceiptOutboxRowAndSucceededRun() throws Exception {
        try (Connection connection = openOutbox()) {
            Fixture fixture = Fixture.createWith(connection);
            long publicationId = DdcAiPersistenceIT.nextId();
            String messageId = "ai-pub-" + publicationId;
            try {
                long policyId = DdcAiPersistenceIT.nextId();
                DdcAiOptimizationConcurrencyIT.insertPolicy(connection, fixture, policyId);
                long runId = DdcAiOptimizationConcurrencyIT.insertRun(connection, fixture, policyId, 1, null);
                connection.commit();

                insertPublication(connection, fixture, publicationId);
                insertAiOutbox(connection, fixture, messageId);
                markAutoPublished(connection, fixture, runId, publicationId);
                connection.commit();

                assertThat(count(connection, "SELECT count(*) FROM ddc_ai_publication_t0 WHERE tenant_id = ? "
                        + "AND id = ?", fixture.tenantId(), publicationId)).isEqualTo(1);
                assertThat(count(connection, "SELECT count(*) FROM egon_outbox.egon_cola_outbox_message "
                        + "WHERE channel = 'tianshu.ai-publication' AND message_id = ?", messageId)).isEqualTo(1);
                assertThat(count(connection, "SELECT count(*) FROM ddc_ai_optimization_run_t0 WHERE tenant_id = ? "
                        + "AND id = ? AND state = 'SUCCEEDED' AND auto_published AND publication_id = ?",
                        fixture.tenantId(), runId, publicationId)).isEqualTo(1);
            } finally {
                deleteOutbox(connection, messageId);
                fixture.cleanupWith(connection);
            }
        }
    }

    @Test
    void outboxEnqueueFailureRollsBackPublication() throws Exception {
        try (Connection connection = openOutbox()) {
            Fixture fixture = Fixture.createWith(connection);
            long publicationId = DdcAiPersistenceIT.nextId();
            String messageId = "ai-pub-" + publicationId;
            try {
                insertAiOutbox(connection, fixture, messageId);
                connection.commit();

                insertPublication(connection, fixture, publicationId);
                assertThatThrownBy(() -> insertAiOutbox(connection, fixture, messageId))
                        .isInstanceOf(SQLException.class)
                        .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23505"));
                connection.rollback();

                assertThat(count(connection, "SELECT count(*) FROM ddc_ai_publication_t0 WHERE tenant_id = ? "
                        + "AND id = ?", fixture.tenantId(), publicationId)).isZero();
            } finally {
                deleteOutbox(connection, messageId);
                fixture.cleanupWith(connection);
            }
        }
    }

    private static void insertPublication(Connection connection, Fixture fixture, long publicationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ddc_ai_publication_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                        + "update_time, deleted_at, version, resource_id, operation, request_id, request_sha256, "
                        + "previous_version_id, target_version_id, source_version_id, result_revision, target_created) "
                        + "VALUES (?, ?, 'system:ai-optimizer', clock_timestamp(), 'system:ai-optimizer', "
                        + "clock_timestamp(), NULL, 0, ?, 'PUBLISH', ?, ?, NULL, ?, NULL, 1, TRUE)")) {
            statement.setLong(1, publicationId);
            statement.setLong(2, fixture.tenantId());
            statement.setLong(3, fixture.resourceId());
            statement.setString(4, String.format("opt-%d-%019d", fixture.tenantId() % 1000, publicationId));
            statement.setString(5, "c".repeat(64));
            statement.setLong(6, fixture.versionId());
            statement.executeUpdate();
        }
    }

    private static void insertAiOutbox(Connection connection, Fixture fixture, String messageId) throws SQLException {
        String destination = DdcRedisKeys.aiTopic(fixture.tenantId(), fixture.bizCode(), "it-env", "it-app");
        assertThat(DdcOutboxConfiguration.DdcAiPublicationOutboxRouteDTO.TOPIC_PATTERN.matcher(destination).matches())
                .isTrue();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO egon_outbox.egon_cola_outbox_message (id, message_id, idempotency_key, "
                        + "message_fingerprint, channel, destination, payload, content_type, schema_version, "
                        + "headers_json, status, attempt_count, max_attempts, next_attempt_at, created_at, updated_at, "
                        + "tenant_id, create_user_id, create_time, update_user_id, update_time, deleted_at, version) "
                        + "VALUES (?, ?, ?, ?, 'tianshu.ai-publication', ?, '{}', 'application/json', '1', '{}', "
                        + "'PENDING', 0, 10, clock_timestamp(), clock_timestamp(), clock_timestamp(), 0, "
                        + "'system:outbox', clock_timestamp(), 'system:outbox', clock_timestamp(), NULL, 0)")) {
            statement.setLong(1, DdcAiPersistenceIT.nextId());
            statement.setString(2, messageId);
            statement.setString(3, messageId);
            statement.setString(4, "d".repeat(64));
            statement.setString(5, destination);
            statement.executeUpdate();
        }
    }

    private static void markAutoPublished(Connection connection, Fixture fixture, long runId, long publicationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_ai_optimization_run_t0 SET state = 'SUCCEEDED', produced_version_id = ?, "
                        + "publication_id = ?, auto_published = TRUE, finished_at = clock_timestamp(), "
                        + "version = version + 1 WHERE tenant_id = ? AND id = ? AND state = 'PENDING'")) {
            statement.setLong(1, fixture.versionId());
            statement.setLong(2, publicationId);
            statement.setLong(3, fixture.tenantId());
            statement.setLong(4, runId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static void deleteOutbox(Connection connection, String messageId) throws SQLException {
        connection.rollback();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM egon_outbox.egon_cola_outbox_message WHERE message_id = ?")) {
            statement.setString(1, messageId);
            statement.executeUpdate();
        }
        connection.commit();
    }

    private static long count(Connection connection, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < args.length; index++) statement.setObject(index + 1, args[index]);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private static Connection openOutbox() throws SQLException {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires an explicitly configured isolated PostgreSQL/Outbox target");
        String url = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_JDBC_URL");
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_JDBC_URL must target PostgreSQL");
        }
        Connection connection = DriverManager.getConnection(url,
                requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_DB_USERNAME"),
                requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_OUTBOX_DB_PASSWORD"));
        connection.setAutoCommit(false);
        return connection;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for this IT");
        return value;
    }
}
