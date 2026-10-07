package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcLifecycleUniqueIT {

    @Test
    void lifecycleIndexesRejectSameMicrosecondDeleteRestoreAndDuplicateLiveRows() throws Exception {
        long tenantId = DdcAiPersistenceIT.nextId();
        long liveNamespaceId = DdcAiPersistenceIT.nextId();
        long firstDeletedId = DdcAiPersistenceIT.nextId();
        long duplicateDeletedId = DdcAiPersistenceIT.nextId();
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String bizCode = "it-biz-" + suffix;
        String namespaceCode = "it-space-" + suffix;
        String deletedAt = "2026-10-01 10:11:12.123456";

        try (Connection connection = DdcAiPersistenceIT.openPostgres()) {
            connection.setAutoCommit(false);
            try {
                DdcAiPersistenceIT.insertNamespace(connection, tenantId, liveNamespaceId, bizCode, namespaceCode);
                insertNamespace(connection, tenantId, firstDeletedId, bizCode, namespaceCode, deletedAt);

                var restoreSavepoint = connection.setSavepoint();
                try {
                    updateDeletedAt(connection, tenantId, firstDeletedId, null);
                    throw new AssertionError("restore must conflict with the live namespace");
                } catch (SQLException failure) {
                    assertSqlState(failure, "23505");
                } finally {
                    connection.rollback(restoreSavepoint);
                }

                var duplicateDeleteSavepoint = connection.setSavepoint();
                try {
                    insertNamespace(connection, tenantId, duplicateDeletedId, bizCode, namespaceCode, deletedAt);
                    throw new AssertionError("same-key deletes at one microsecond must conflict");
                } catch (SQLException failure) {
                    assertSqlState(failure, "23505");
                } finally {
                    connection.rollback(duplicateDeleteSavepoint);
                }

                var duplicateLiveSavepoint = connection.setSavepoint();
                try {
                    DdcAiPersistenceIT.insertNamespace(connection, tenantId, duplicateDeletedId,
                            bizCode, namespaceCode);
                    throw new AssertionError("only one live namespace may own the lifecycle key");
                } catch (SQLException failure) {
                    assertSqlState(failure, "23505");
                } finally {
                    connection.rollback(duplicateLiveSavepoint);
                }
            } finally {
                connection.rollback();
            }
        }
    }

    private static void insertNamespace(Connection connection, long tenantId, long id,
                                        String bizCode, String namespaceCode, String deletedAt) throws SQLException {
        String sql = "INSERT INTO ddc_namespace_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                + "update_time, deleted_at, version, public_id, namespace, description, enabled, created_at, "
                + "updated_at, namespace_code, biz_code) VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, "
                + "'step24-it', CURRENT_TIMESTAMP, ?::timestamp, 0, ?, ?, '', TRUE, CURRENT_TIMESTAMP, "
                + "CURRENT_TIMESTAMP, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, tenantId);
            statement.setString(3, deletedAt);
            statement.setString(4, UUID.randomUUID().toString());
            statement.setString(5, namespaceCode);
            statement.setString(6, namespaceCode);
            statement.setString(7, bizCode);
            statement.executeUpdate();
        }
    }

    private static void updateDeletedAt(Connection connection, long tenantId, long id, String deletedAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_namespace_t0 SET deleted_at = ?::timestamp WHERE tenant_id = ? AND id = ?")) {
            statement.setString(1, deletedAt);
            statement.setLong(2, tenantId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    private static void assertSqlState(SQLException failure, String expected) {
        if (!expected.equals(failure.getSQLState())) {
            throw new AssertionError("Expected SQLState " + expected + " but received " + failure.getSQLState(),
                    failure);
        }
    }
}
