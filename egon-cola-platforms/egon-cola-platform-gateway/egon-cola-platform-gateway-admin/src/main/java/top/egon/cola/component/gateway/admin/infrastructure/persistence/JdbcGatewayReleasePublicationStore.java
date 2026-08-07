package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

@Repository
public class JdbcGatewayReleasePublicationStore
        implements GatewayReleasePublicationStore {

    private final JdbcTemplate jdbc;

    public JdbcGatewayReleasePublicationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void insertAll(List<PublicationRecord> operations) {
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException(
                    "publication operations must not be empty"
            );
        }
        operations.forEach(this::insert);
    }

    @Override
    public List<PublicationRecord> findAttempt(
            String releaseId,
            int attemptNo) {
        return jdbc.query("""
                SELECT release_id, attempt_no, phase_order, phase_type,
                       config_key, content_value, content_sha256,
                       expected_version, change_id, ddc_target_version,
                       ddc_status, error_code, error_message,
                       created_at, updated_at
                  FROM gateway_release_publication
                 WHERE release_id = ? AND attempt_no = ?
                 ORDER BY phase_order
                """, (result, row) -> publication(result),
                releaseId, attemptNo);
    }

    @Override
    public Optional<PublicationRecord> nextIncomplete(
            String releaseId,
            int attemptNo) {
        return jdbc.query("""
                SELECT release_id, attempt_no, phase_order, phase_type,
                       config_key, content_value, content_sha256,
                       expected_version, change_id, ddc_target_version,
                       ddc_status, error_code, error_message,
                       created_at, updated_at
                  FROM gateway_release_publication
                 WHERE release_id = ? AND attempt_no = ?
                   AND ddc_status <> 'SUCCESS'
                 ORDER BY phase_order
                 LIMIT 1
                """, (result, row) -> publication(result),
                releaseId, attemptNo).stream().findFirst();
    }

    @Override
    public List<ChunkCleanupCandidate> findChunkCleanupCandidates(
            Instant successorActivatedBefore) {
        return jdbc.query("""
                SELECT publication.change_id,
                       publication.release_id,
                       'gateway-engine-' || (
                           content.canonical_snapshot
                               -> 'content' ->> 'gatewayGroupCode'
                       ) AS app_code,
                       content.canonical_snapshot
                           -> 'content' ->> 'env' AS env,
                       content.canonical_snapshot
                           -> 'content' ->> 'namespace' AS namespace,
                       publication.config_key,
                       publication.ddc_target_version
                  FROM gateway_release_publication publication
                  JOIN gateway_release old_release
                    ON old_release.id = publication.release_id
                  JOIN gateway_release_content content
                    ON content.release_id = publication.release_id
                 WHERE publication.phase_type = 'CHUNK'
                   AND publication.ddc_status = 'SUCCESS'
                   AND publication.ddc_target_version IS NOT NULL
                   AND COALESCE(publication.error_code, '')
                       <> 'CHUNK_GC_DELETED'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM gateway_draft active_draft
                        WHERE active_draft.based_on_release_id =
                              publication.release_id
                   )
                   AND EXISTS (
                       SELECT 1
                         FROM gateway_release successor
                         JOIN gateway_release_publication activation
                           ON activation.release_id = successor.id
                          AND activation.phase_type = 'ACTIVATION'
                          AND activation.ddc_status = 'SUCCESS'
                        WHERE successor.gateway_group_id =
                              old_release.gateway_group_id
                          AND successor.created_at > old_release.created_at
                          AND activation.updated_at <= ?
                   )
                 ORDER BY old_release.created_at, publication.phase_order
                """, (result, row) -> new ChunkCleanupCandidate(
                result.getString("change_id"),
                result.getString("release_id"),
                result.getString("app_code"),
                result.getString("env"),
                result.getString("namespace"),
                result.getString("config_key"),
                result.getLong("ddc_target_version")
        ), timestamp(successorActivatedBefore));
    }

    @Override
    public void resolveDocument(
            String changeId,
            long expectedVersion,
            String documentContent,
            Instant now) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion must not be negative"
            );
        }
        if (documentContent == null || documentContent.isBlank()) {
            throw new IllegalArgumentException(
                    "documentContent must not be blank"
            );
        }
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET expected_version = ?, content_value = ?,
                       ddc_status = 'RESOLVED', updated_at = ?
                 WHERE change_id = ? AND ddc_status <> 'SUCCESS'
                """, expectedVersion, documentContent,
                timestamp(now), changeId);
        requireChanged(
                changed,
                "successful publication cannot be resolved again"
        );
    }

    @Override
    public void markSubmitted(String changeId, Instant now) {
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET ddc_status = 'SUBMITTED', updated_at = ?
                 WHERE change_id = ? AND ddc_status = 'RESOLVED'
                   AND expected_version IS NOT NULL
                """, timestamp(now), changeId);
        requireChanged(
                changed,
                "publication must be RESOLVED before submission"
        );
    }

    @Override
    public void markResult(
            String changeId,
            Long targetVersion,
            PublicationStatus status,
            String errorCode,
            String errorMessage,
            Instant now) {
        if (status == null || !status.terminalResult()) {
            throw new IllegalArgumentException(
                    "publication result status must be terminal"
            );
        }
        if (status == PublicationStatus.SUCCESS && targetVersion == null) {
            throw new IllegalArgumentException(
                    "successful publication requires targetVersion"
            );
        }
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET ddc_target_version = ?, ddc_status = ?,
                       error_code = ?, error_message = ?, updated_at = ?
                 WHERE change_id = ? AND ddc_status IN (
                       'RESOLVED', 'SUBMITTED', 'PARTIAL_SUCCESS',
                       'TIMEOUT', 'UNKNOWN'
                 )
                """,
                targetVersion,
                status.name(),
                errorCode,
                errorMessage,
                timestamp(now),
                changeId
        );
        requireChanged(changed, "publication result cannot be recorded");
    }

    @Override
    public void markChunkCleaned(String changeId, Instant now) {
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET error_code = 'CHUNK_GC_DELETED',
                       error_message = NULL, updated_at = ?
                 WHERE change_id = ? AND phase_type = 'CHUNK'
                   AND ddc_status = 'SUCCESS'
                   AND ddc_target_version IS NOT NULL
                """, timestamp(now), changeId);
        requireChanged(changed, "cleaned chunk publication was not found");
    }

    private void insert(PublicationRecord operation) {
        if (operation == null) {
            throw new IllegalArgumentException(
                    "publication operation must not be null"
            );
        }
        jdbc.update("""
                INSERT INTO gateway_release_publication(
                    release_id, attempt_no, phase_order, phase_type,
                    config_key, content_value, content_sha256,
                    expected_version, change_id, ddc_target_version,
                    ddc_status, error_code, error_message,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                operation.releaseId(),
                operation.attemptNo(),
                operation.phaseOrder(),
                operation.phaseType().name(),
                operation.configKey(),
                operation.contentValue(),
                operation.contentSha256(),
                operation.expectedVersion(),
                operation.changeId(),
                operation.ddcTargetVersion(),
                operation.status().name(),
                operation.errorCode(),
                operation.errorMessage(),
                timestamp(operation.createdAt()),
                timestamp(operation.updatedAt())
        );
    }

    private PublicationRecord publication(ResultSet result)
            throws SQLException {
        return new PublicationRecord(
                result.getString("release_id"),
                result.getInt("attempt_no"),
                result.getInt("phase_order"),
                PhaseType.valueOf(result.getString("phase_type")),
                result.getString("config_key"),
                result.getString("content_value"),
                result.getString("content_sha256"),
                (Long) result.getObject("expected_version"),
                result.getString("change_id"),
                (Long) result.getObject("ddc_target_version"),
                PublicationStatus.valueOf(result.getString("ddc_status")),
                result.getString("error_code"),
                result.getString("error_message"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    private void requireChanged(int changed, String message) {
        if (changed != 1) {
            throw new IllegalStateException(message);
        }
    }
}
