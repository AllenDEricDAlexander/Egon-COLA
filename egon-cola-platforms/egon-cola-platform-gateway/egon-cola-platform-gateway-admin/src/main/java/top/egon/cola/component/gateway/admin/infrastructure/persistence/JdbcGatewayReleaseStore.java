package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

@Repository
public class JdbcGatewayReleaseStore implements GatewayReleaseStore {

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    public JdbcGatewayReleaseStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(
            ReleaseRecord release,
            CompiledGatewayRelease compiled,
            int attemptNo) {
        jdbc.update("""
                INSERT INTO gateway_release(
                    id, gateway_group_id, draft_revision,
                    based_on_release_id, rollback_of_release_id, status,
                    partial_applied, change_id, validation_report,
                    structured_diff, change_reason, created_at, created_by,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, FALSE, NULL, ?::jsonb,
                          ?::jsonb, ?, ?, ?, ?)
                """,
                release.id(),
                release.gatewayGroupId(),
                release.draftRevision(),
                release.basedOnReleaseId(),
                release.rollbackOfReleaseId(),
                release.status().name(),
                json(release.validationReport()),
                json(release.structuredDiff()),
                release.changeReason(),
                timestamp(release.createdAt()),
                release.createdBy(),
                timestamp(release.updatedAt())
        );
        jdbc.update("""
                INSERT INTO gateway_release_content(
                    release_id, rule_content_sha256, artifact_sha256,
                    canonical_snapshot, activation_content, chunk_manifest,
                    snapshot_size, created_at
                ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
                """,
                release.id(),
                compiled.snapshot().ruleContentSha256(),
                compiled.snapshot().artifactSha256(),
                compiled.snapshotJson(),
                compiled.activationJson(),
                json(compiled.chunkValues()),
                compiled.snapshotJson().getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                ).length,
                timestamp(release.createdAt())
        );
        insertAttempt(
                release.id(),
                attemptNo,
                "PENDING",
                release.createdAt()
        );
    }

    @Override
    public Optional<ReleaseRecord> find(String releaseId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, draft_revision,
                       based_on_release_id, rollback_of_release_id, status,
                       partial_applied, change_id,
                       validation_report::text AS validation_report,
                       structured_diff::text AS structured_diff,
                       change_reason, created_at, created_by, updated_at
                  FROM gateway_release
                 WHERE id = ?
                """, (result, row) -> new ReleaseRecord(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getLong("draft_revision"),
                result.getString("based_on_release_id"),
                result.getString("rollback_of_release_id"),
                GatewayReleaseStatus.valueOf(result.getString("status")),
                result.getBoolean("partial_applied"),
                result.getString("change_id"),
                map(result.getString("validation_report")),
                map(result.getString("structured_diff")),
                result.getString("change_reason"),
                result.getTimestamp("created_at").toInstant(),
                result.getString("created_by"),
                result.getTimestamp("updated_at").toInstant()
        ), releaseId).stream().findFirst();
    }

    @Override
    public List<ReleaseRecord> history(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, draft_revision,
                       based_on_release_id, rollback_of_release_id, status,
                       partial_applied, change_id,
                       validation_report::text AS validation_report,
                       structured_diff::text AS structured_diff,
                       change_reason, created_at, created_by, updated_at
                  FROM gateway_release
                 WHERE gateway_group_id = ?
                 ORDER BY created_at DESC
                """, (result, row) -> release(result), gatewayGroupId);
    }

    @Override
    public List<RecoverableAttempt> recoverable() {
        return jdbc.query("""
                SELECT r.id AS release_id,
                       r.gateway_group_id,
                       p.attempt_no
                  FROM gateway_release r
                  JOIN gateway_release_publication p
                    ON p.release_id = r.id
                 WHERE r.status IN (
                       'PUBLISHING', 'FAILED', 'TIMEOUT', 'UNKNOWN'
                 )
                   AND p.attempt_no = (
                       SELECT MAX(candidate.attempt_no)
                         FROM gateway_release_publication candidate
                        WHERE candidate.release_id = r.id
                   )
                 GROUP BY r.id, r.gateway_group_id,
                          p.attempt_no, r.updated_at
                 ORDER BY r.updated_at
                """, (result, row) -> new RecoverableAttempt(
                result.getString("release_id"),
                result.getString("gateway_group_id"),
                result.getInt("attempt_no")
        ));
    }

    @Override
    public List<AttemptRecord> attempts(String releaseId) {
        Map<Integer, List<TargetRecord>> targets =
                new java.util.LinkedHashMap<>();
        jdbc.query("""
                SELECT attempt_no, instance_id, lease_id, status,
                       applied_version, applied_artifact_sha256, error_code,
                       observed_at
                  FROM gateway_release_target
                 WHERE release_id = ?
                 ORDER BY attempt_no, instance_id, lease_id
                """, result -> {
            targets.computeIfAbsent(
                    result.getInt("attempt_no"),
                    ignored -> new java.util.ArrayList<>()
            ).add(new TargetRecord(
                    result.getString("instance_id"),
                    result.getString("lease_id"),
                    result.getString("status"),
                    (Long) result.getObject("applied_version"),
                    result.getString("applied_artifact_sha256"),
                    result.getString("error_code"),
                    result.getTimestamp("observed_at").toInstant()
            ));
        }, releaseId);
        return jdbc.query("""
                SELECT attempt_no, status, change_id, started_at,
                       completed_at, error_code, error_message
                  FROM gateway_release_attempt
                 WHERE release_id = ?
                 ORDER BY attempt_no DESC
                """, (result, row) -> new AttemptRecord(
                result.getInt("attempt_no"),
                result.getString("status"),
                result.getString("change_id"),
                result.getTimestamp("started_at").toInstant(),
                result.getTimestamp("completed_at") == null
                        ? null
                        : result.getTimestamp("completed_at").toInstant(),
                result.getString("error_code"),
                result.getString("error_message"),
                List.copyOf(targets.getOrDefault(
                        result.getInt("attempt_no"),
                        List.of()
                ))
        ), releaseId);
    }

    @Override
    public int latestAttempt(String releaseId) {
        Integer attempt = jdbc.queryForObject("""
                SELECT COALESCE(MAX(attempt_no), 0)
                  FROM gateway_release_attempt
                 WHERE release_id = ?
                """, Integer.class, releaseId);
        if (attempt == null || attempt == 0) {
            throw new IllegalArgumentException(
                    "release attempt was not found"
            );
        }
        return attempt;
    }

    @Override
    public CompiledGatewayRelease loadCompiled(String releaseId) {
        return jdbc.query("""
                SELECT canonical_snapshot::text AS canonical_snapshot,
                       activation_content::text AS activation_content,
                       chunk_manifest::text AS chunk_manifest
                  FROM gateway_release_content
                 WHERE release_id = ?
                """, result -> {
            if (!result.next()) {
                throw new IllegalArgumentException(
                        "release content was not found"
                );
            }
            String snapshotJson = result.getString("canonical_snapshot");
            String activationJson = result.getString("activation_content");
            return new CompiledGatewayRelease(
                    read(snapshotJson, GatewayRuleSnapshot.class),
                    snapshotJson,
                    read(activationJson, GatewayRuleActivation.class),
                    activationJson,
                    stringMap(result.getString("chunk_manifest"))
            );
        }, releaseId);
    }

    @Override
    public int nextAttempt(String releaseId, Instant now) {
        Integer maximum = jdbc.queryForObject("""
                SELECT COALESCE(MAX(attempt_no), 0)
                  FROM gateway_release_attempt
                 WHERE release_id = ?
                """, Integer.class, releaseId);
        int attempt = maximum == null ? 1 : maximum + 1;
        insertAttempt(releaseId, attempt, "PENDING", now);
        return attempt;
    }

    @Override
    public void beginAttempt(
            String releaseId,
            int attemptNo,
            Instant now) {
        jdbc.update("""
                UPDATE gateway_release_attempt
                   SET status = 'PUBLISHING', started_at = ?,
                       completed_at = NULL, error_code = NULL,
                       error_message = NULL
                 WHERE release_id = ? AND attempt_no = ?
                """, timestamp(now), releaseId, attemptNo);
        jdbc.update("""
                UPDATE gateway_release
                   SET status = 'PUBLISHING', updated_at = ?
                 WHERE id = ?
                """, timestamp(now), releaseId);
    }

    @Override
    public void completeAttempt(
            String releaseId,
            int attemptNo,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            String errorCode,
            String errorMessage,
            List<TargetRecord> targets,
            Instant now) {
        jdbc.update("""
                UPDATE gateway_release_attempt
                   SET status = ?, change_id = ?, completed_at = ?,
                       error_code = ?, error_message = ?
                 WHERE release_id = ? AND attempt_no = ?
                """,
                status.name(),
                changeId,
                timestamp(now),
                errorCode,
                errorMessage,
                releaseId,
                attemptNo
        );
        jdbc.update("""
                UPDATE gateway_release
                   SET status = ?, partial_applied = ?, change_id = ?,
                       updated_at = ?
                 WHERE id = ?
                """,
                status.name(),
                partialApplied,
                changeId,
                timestamp(now),
                releaseId
        );
        targets.forEach(target -> jdbc.update("""
                INSERT INTO gateway_release_target(
                    release_id, attempt_no, instance_id, lease_id, status,
                    applied_version, applied_artifact_sha256, error_code,
                    observed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (
                    release_id, attempt_no, instance_id, lease_id
                ) DO UPDATE SET status = EXCLUDED.status,
                                applied_version = EXCLUDED.applied_version,
                                applied_artifact_sha256 =
                                    EXCLUDED.applied_artifact_sha256,
                                error_code = EXCLUDED.error_code,
                                observed_at = EXCLUDED.observed_at
                """,
                releaseId,
                attemptNo,
                target.instanceId(),
                target.leaseId(),
                target.status(),
                target.appliedVersion(),
                target.appliedArtifactSha256(),
                target.errorCode(),
                timestamp(target.observedAt())
        ));
    }

    @Override
    public boolean hasReleaseInProgress(String gatewayGroupId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM gateway_release
                 WHERE gateway_group_id = ?
                   AND status IN (
                       'CREATED', 'VALIDATING', 'READY', 'PUBLISHING'
                   )
                """, Integer.class, gatewayGroupId);
        return count != null && count > 0;
    }

    private void insertAttempt(
            String releaseId,
            int attemptNo,
            String status,
            Instant now) {
        jdbc.update("""
                INSERT INTO gateway_release_attempt(
                    release_id, attempt_no, status, started_at
                ) VALUES (?, ?, ?, ?)
                """, releaseId, attemptNo, status, timestamp(now));
    }

    private ReleaseRecord release(java.sql.ResultSet result)
            throws java.sql.SQLException {
        return new ReleaseRecord(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getLong("draft_revision"),
                result.getString("based_on_release_id"),
                result.getString("rollback_of_release_id"),
                GatewayReleaseStatus.valueOf(result.getString("status")),
                result.getBoolean("partial_applied"),
                result.getString("change_id"),
                map(result.getString("validation_report")),
                map(result.getString("structured_diff")),
                result.getString("change_reason"),
                result.getTimestamp("created_at").toInstant(),
                result.getString("created_by"),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "release value cannot be serialized",
                    failure
            );
        }
    }

    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored release metadata is invalid",
                    failure
            );
        }
    }

    private Map<String, String> stringMap(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, String>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored chunk manifest is invalid",
                    failure
            );
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored release content is invalid",
                    failure
            );
        }
    }
}
