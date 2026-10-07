package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAiPromotionIT {

    @Test
    void sameTenantPromotionCanCopyTheWholeSkillSnapshotWithoutChangingEitherDraft() throws Exception {
        long tenantId = DdcAiPersistenceIT.nextId();
        long sourceNamespaceId = DdcAiPersistenceIT.nextId();
        long targetNamespaceId = DdcAiPersistenceIT.nextId();
        long sourceResourceId = DdcAiPersistenceIT.nextId();
        long targetResourceId = DdcAiPersistenceIT.nextId();
        long sourceVersionId = DdcAiPersistenceIT.nextId();
        long targetVersionId = DdcAiPersistenceIT.nextId();
        long publicationId = DdcAiPersistenceIT.nextId();
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String resourceKey = "skill-" + suffix;
        String sourceDraft = "source draft\n";
        String targetDraft = "target draft\n";
        String sourceSnapshot = "published source\n";
        String checksum = "b".repeat(64);
        long sourceBytes = sourceSnapshot.getBytes(StandardCharsets.UTF_8).length;

        try (Connection connection = DdcAiPersistenceIT.openPostgres()) {
            connection.setAutoCommit(false);
            try {
                DdcAiPersistenceIT.insertNamespace(connection, tenantId, sourceNamespaceId,
                        "source-biz-" + suffix, "source-namespace-" + suffix);
                DdcAiPersistenceIT.insertNamespace(connection, tenantId, targetNamespaceId,
                        "target-biz-" + suffix, "target-namespace-" + suffix);
                DdcAiPersistenceIT.insertResource(connection, sourceResourceId, tenantId, sourceNamespaceId,
                        "source-biz-" + suffix, "dev", "source-app", "SKILL", resourceKey, null);
                DdcAiPersistenceIT.insertResource(connection, targetResourceId, tenantId, targetNamespaceId,
                        "target-biz-" + suffix, "prod", "target-app", "SKILL", resourceKey, null);
                insertDraftFile(connection, tenantId, sourceResourceId, "source.txt", sourceDraft);
                insertDraftFile(connection, tenantId, targetResourceId, "target.txt", targetDraft);

                connection.createStatement().execute("SET CONSTRAINTS ALL DEFERRED");
                insertSkillVersion(connection, sourceVersionId, tenantId, sourceResourceId, 1L,
                        sourceSnapshot, checksum, sourceBytes, null);
                insertSkillNode(connection, tenantId, sourceVersionId, "README.txt", sourceSnapshot, checksum,
                        sourceBytes);
                insertSkillVersion(connection, targetVersionId, tenantId, targetResourceId, 1L,
                        sourceSnapshot, checksum, sourceBytes, sourceVersionId);
                insertSkillNode(connection, tenantId, targetVersionId, "README.txt", sourceSnapshot, checksum,
                        sourceBytes);
                insertPromotionReceipt(connection, publicationId, tenantId, targetResourceId,
                        targetVersionId, sourceVersionId, "promote_" + suffix);
                updateCurrentVersion(connection, tenantId, targetResourceId, targetVersionId);
                connection.createStatement().execute("SET CONSTRAINTS ALL IMMEDIATE");

                assertThat(readText(connection, "SELECT content FROM ddc_ai_draft_node_t0 "
                        + "WHERE tenant_id = " + tenantId + " AND resource_id = " + sourceResourceId
                        + " AND path = 'source.txt' AND deleted_at IS NULL")).isEqualTo(sourceDraft);
                assertThat(readText(connection, "SELECT content FROM ddc_ai_draft_node_t0 "
                        + "WHERE tenant_id = " + tenantId + " AND resource_id = " + targetResourceId
                        + " AND path = 'target.txt' AND deleted_at IS NULL")).isEqualTo(targetDraft);
                assertThat(readText(connection, "SELECT content FROM ddc_ai_version_node_t0 "
                        + "WHERE tenant_id = " + tenantId + " AND version_id = " + targetVersionId
                        + " AND path = 'README.txt' AND deleted_at IS NULL")).isEqualTo(sourceSnapshot);
                assertThat(readLong(connection, "SELECT current_version_id FROM ddc_ai_resource_t0 "
                        + "WHERE tenant_id = " + tenantId + " AND id = " + targetResourceId)).isEqualTo(targetVersionId);
                assertThat(readLong(connection, "SELECT source_version_id FROM ddc_ai_version_t0 "
                        + "WHERE tenant_id = " + tenantId + " AND id = " + targetVersionId)).isEqualTo(sourceVersionId);
                assertThat(readLong(connection, "SELECT count(*) FROM ddc_ai_publication_t0 "
                        + "WHERE tenant_id = " + tenantId + " AND id = " + publicationId
                        + " AND operation = 'PROMOTE' AND target_created = TRUE")).isEqualTo(1L);
            } finally {
                connection.rollback();
            }
        }
    }

    private static void insertDraftFile(Connection connection, long tenantId, long resourceId,
                                        String path, String content) throws Exception {
        String sql = "INSERT INTO ddc_ai_draft_node_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                + "update_time, deleted_at, version, resource_id, parent_path, path, node_type, content, "
                + "content_sha256, content_bytes) VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, 'step24-it', "
                + "CURRENT_TIMESTAMP, NULL, 0, ?, '', ?, 'FILE', ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, DdcAiPersistenceIT.nextId());
            statement.setLong(2, tenantId);
            statement.setLong(3, resourceId);
            statement.setString(4, path);
            statement.setString(5, content);
            statement.setString(6, "c".repeat(64));
            statement.setLong(7, content.getBytes(StandardCharsets.UTF_8).length);
            statement.executeUpdate();
        }
    }

    private static void insertSkillVersion(Connection connection, long versionId, long tenantId,
                                           long resourceId, long versionNo, String content, String checksum,
                                           long contentBytes, Long sourceVersionId) throws Exception {
        String sql = "INSERT INTO ddc_ai_version_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                + "update_time, deleted_at, version, resource_id, version_no, kind, display_name, description, "
                + "prompt_text, content_sha256, content_bytes, node_count, source_version_id, change_note) "
                + "VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, 'step24-it', CURRENT_TIMESTAMP, NULL, 0, ?, ?, "
                + "'SKILL', 'Copied skill', '', NULL, ?, ?, 2, ?, 'integration snapshot')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, versionId);
            statement.setLong(2, tenantId);
            statement.setLong(3, resourceId);
            statement.setLong(4, versionNo);
            statement.setString(5, checksum);
            statement.setLong(6, contentBytes);
            if (sourceVersionId == null) statement.setNull(7, java.sql.Types.BIGINT);
            else statement.setLong(7, sourceVersionId);
            statement.executeUpdate();
        }
    }

    private static void insertSkillNode(Connection connection, long tenantId, long versionId,
                                        String path, String content, String checksum, long contentBytes)
            throws Exception {
        String rootSql = "INSERT INTO ddc_ai_version_node_t0 (id, tenant_id, create_user_id, create_time, "
                + "update_user_id, update_time, deleted_at, version, version_id, parent_path, path, node_type, "
                + "content, content_sha256, content_bytes) VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, "
                + "'step24-it', CURRENT_TIMESTAMP, NULL, 0, ?, NULL, '', 'DIRECTORY', NULL, NULL, 0)";
        try (PreparedStatement statement = connection.prepareStatement(rootSql)) {
            statement.setLong(1, DdcAiPersistenceIT.nextId());
            statement.setLong(2, tenantId);
            statement.setLong(3, versionId);
            statement.executeUpdate();
        }
        String fileSql = "INSERT INTO ddc_ai_version_node_t0 (id, tenant_id, create_user_id, create_time, "
                + "update_user_id, update_time, deleted_at, version, version_id, parent_path, path, node_type, "
                + "content, content_sha256, content_bytes) VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, "
                + "'step24-it', CURRENT_TIMESTAMP, NULL, 0, ?, '', ?, 'FILE', ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(fileSql)) {
            statement.setLong(1, DdcAiPersistenceIT.nextId());
            statement.setLong(2, tenantId);
            statement.setLong(3, versionId);
            statement.setString(4, path);
            statement.setString(5, content);
            statement.setString(6, checksum);
            statement.setLong(7, contentBytes);
            statement.executeUpdate();
        }
    }

    private static void insertPromotionReceipt(Connection connection, long publicationId, long tenantId,
                                               long targetResourceId, long targetVersionId,
                                               long sourceVersionId, String requestId) throws Exception {
        String sql = "INSERT INTO ddc_ai_publication_t0 (id, tenant_id, create_user_id, create_time, "
                + "update_user_id, update_time, deleted_at, version, resource_id, operation, request_id, "
                + "request_sha256, previous_version_id, target_version_id, source_version_id, result_revision, "
                + "target_created) VALUES (?, ?, 'step24-it', CURRENT_TIMESTAMP, 'step24-it', CURRENT_TIMESTAMP, "
                + "NULL, 0, ?, 'PROMOTE', ?, ?, NULL, ?, ?, 1, TRUE)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, publicationId);
            statement.setLong(2, tenantId);
            statement.setLong(3, targetResourceId);
            statement.setString(4, requestId);
            statement.setString(5, "d".repeat(64));
            statement.setLong(6, targetVersionId);
            statement.setLong(7, sourceVersionId);
            statement.executeUpdate();
        }
    }

    private static void updateCurrentVersion(Connection connection, long tenantId, long resourceId,
                                             long versionId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_ai_resource_t0 SET current_version_id = ?, last_version_no = 1 "
                        + "WHERE tenant_id = ? AND id = ?")) {
            statement.setLong(1, versionId);
            statement.setLong(2, tenantId);
            statement.setLong(3, resourceId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static String readText(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) return null;
            return result.getString(1);
        }
    }

    private static Long readLong(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) return null;
            long value = result.getLong(1);
            return result.wasNull() ? null : value;
        }
    }
}
