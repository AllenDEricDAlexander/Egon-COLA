package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiPersistenceIT {

    @Test
    void initializedPostgresRejectsAiRowsWithMissingTenantScopedParents() throws Exception {
        try (Connection connection = openPostgres()) {
            connection.setAutoCommit(false);
            try {
                assertThat(constraintExists(connection, "ddc_ai_resource_t0", "fk_ai_resource_namespace")).isTrue();
                assertThat(constraintExists(connection, "ddc_ai_version_t0", "fk_ai_version_resource")).isTrue();
                assertThat(constraintExists(connection, "ddc_ai_version_node_t0", "fk_ai_version_node_version"))
                        .isTrue();

                long tenantId = nextId();
                long resourceId = nextId();
                Savepoint resourceSavepoint = connection.setSavepoint();
                try {
                    assertThatThrownBy(() -> insertResource(connection, resourceId, tenantId, nextId(),
                            "it-biz", "it-env", "it-app", "PROMPT", uniqueKey(), "draft"))
                            .isInstanceOf(SQLException.class)
                            .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23503"));
                } finally {
                    connection.rollback(resourceSavepoint);
                }

                Savepoint versionSavepoint = connection.setSavepoint();
                try {
                    assertThatThrownBy(() -> insertVersion(connection, nextId(), tenantId, resourceId,
                            "PROMPT", "snapshot"))
                            .isInstanceOf(SQLException.class)
                            .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23503"));
                } finally {
                    connection.rollback(versionSavepoint);
                }
            } finally {
                connection.rollback();
            }
        }
    }

    static Connection openPostgres() throws SQLException {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires the explicit isolated PostgreSQL/Redis integration profile");
        String jdbcUrl = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_JDBC_URL");
        if (!jdbcUrl.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("TIANSHU_AI_PERSISTENCE_IT_JDBC_URL must target PostgreSQL");
        }
        return DriverManager.getConnection(jdbcUrl,
                requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_DB_USERNAME"),
                requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_DB_PASSWORD"));
    }

    static long nextId() {
        return ThreadLocalRandom.current().nextLong(10_000_000_000_000L, Long.MAX_VALUE);
    }

    static String uniqueKey() {
        return "it-" + UUID.randomUUID().toString().replace("-", "");
    }

    static void insertNamespace(Connection connection, long tenantId, long namespaceId,
                                String bizCode, String namespaceCode) throws SQLException {
        String sql = "INSERT INTO ddc_namespace_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                + "update_time, deleted_at, version, public_id, namespace, description, enabled, created_at, "
                + "updated_at, namespace_code, biz_code) VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, "
                + "'step24-it', CURRENT_TIMESTAMP, NULL, 0, ?, ?, '', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, namespaceId);
            statement.setLong(2, tenantId);
            statement.setString(3, uniqueKey());
            statement.setString(4, namespaceCode);
            statement.setString(5, namespaceCode);
            statement.setString(6, bizCode);
            statement.executeUpdate();
        }
    }

    static void insertResource(Connection connection, long resourceId, long tenantId, long namespaceId,
                               String bizCode, String env, String appCode, String kind,
                               String resourceKey, String draftText) throws SQLException {
        String sql = "INSERT INTO ddc_ai_resource_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                + "update_time, deleted_at, version, biz_code, namespace_id, env_code, app_code, kind, resource_key, "
                + "display_name, description, draft_prompt, last_version_no, current_version_id) "
                + "VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, 'step24-it', CURRENT_TIMESTAMP, NULL, 0, "
                + "?, ?, ?, ?, ?, ?, 'Integration resource', '', ?, 0, NULL)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, resourceId);
            statement.setLong(2, tenantId);
            statement.setString(3, bizCode);
            statement.setLong(4, namespaceId);
            statement.setString(5, env);
            statement.setString(6, appCode);
            statement.setString(7, kind);
            statement.setString(8, resourceKey);
            statement.setString(9, "PROMPT".equals(kind) ? draftText : null);
            statement.executeUpdate();
        }
    }

    static void insertVersion(Connection connection, long versionId, long tenantId, long resourceId,
                              String kind, String promptText) throws SQLException {
        String sql = "INSERT INTO ddc_ai_version_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                + "update_time, deleted_at, version, resource_id, version_no, kind, display_name, description, "
                + "prompt_text, content_sha256, content_bytes, node_count, source_version_id, change_note) "
                + "VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, 'step24-it', CURRENT_TIMESTAMP, NULL, 0, ?, 1, "
                + "?, 'Integration snapshot', '', ?, ?, ?, 0, NULL, 'integration fixture')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, versionId);
            statement.setLong(2, tenantId);
            statement.setLong(3, resourceId);
            statement.setString(4, kind);
            statement.setString(5, promptText);
            statement.setString(6, "a".repeat(64));
            statement.setLong(7, promptText.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            statement.executeUpdate();
        }
    }

    static void softDeleteResource(Connection connection, long tenantId, long resourceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_ai_resource_t0 SET deleted_at = clock_timestamp() AT TIME ZONE 'UTC' "
                        + "WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL")) {
            statement.setLong(1, tenantId);
            statement.setLong(2, resourceId);
            statement.executeUpdate();
        }
    }

    static void softDeleteNamespace(Connection connection, long tenantId, long namespaceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_namespace_t0 SET deleted_at = clock_timestamp() AT TIME ZONE 'UTC' "
                        + "WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL")) {
            statement.setLong(1, tenantId);
            statement.setLong(2, namespaceId);
            statement.executeUpdate();
        }
    }

    private static boolean constraintExists(Connection connection, String table, String constraint)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = t.relnamespace "
                        + "WHERE n.nspname = 'public' AND t.relname = ? AND c.conname = ?")) {
            statement.setString(1, table);
            statement.setString(2, constraint);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when TIANSHU_AI_PERSISTENCE_IT_ENABLED=true");
        }
        return value;
    }
}
