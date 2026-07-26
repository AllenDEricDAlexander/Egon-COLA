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
    public void resolveVersion(
            String changeId,
            long expectedVersion,
            Instant now) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion must not be negative"
            );
        }
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET expected_version = ?, ddc_status = 'RESOLVED',
                       updated_at = ?
                 WHERE change_id = ? AND ddc_status = 'PLANNED'
                """, expectedVersion, now, changeId);
        requireChanged(
                changed,
                "publication must be PLANNED before version resolution"
        );
    }

    @Override
    public void markSubmitted(String changeId, Instant now) {
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET ddc_status = 'SUBMITTED', updated_at = ?
                 WHERE change_id = ? AND ddc_status = 'RESOLVED'
                   AND expected_version IS NOT NULL
                """, now, changeId);
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
                now,
                changeId
        );
        requireChanged(changed, "publication result cannot be recorded");
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
                operation.createdAt(),
                operation.updatedAt()
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
