package top.egon.cola.component.gateway.admin.openapi.repository.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;
import top.egon.cola.component.gateway.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.gateway.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.gateway.admin.openapi.repository.GatewayOpenApiSyncRepository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JDBC persistence adapter for per-Group OpenAPI synchronization state.
 *
 * <p>中文：使用 revision 条件更新实现同步行的乐观 CAS，不持有跨网络事务。
 */
@Repository("gatewayOpenApiSyncRepository")
public class JdbcGatewayOpenApiSyncRepository
        implements GatewayOpenApiSyncRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, application_id, build_id, artifact_version,
                   openapi_group, provider_service_name, provider_group,
                   provider_version, status, latest_snapshot_id,
                   definition_set_id, last_instance_id, attempt_count,
                   last_error_code, last_error_message, first_discovered_at,
                   last_attempt_at, last_success_at, next_retry_at, revision,
                   updated_at
              FROM gateway_openapi_sync_state
            """;

    private static final String CLAIMABLE_STATES = """
            ('DISCOVERED', 'FETCH_FAILED', 'INGEST_FAILED', 'STALE')
            """;

    private final JdbcTemplate jdbc;

    /**
     * Creates the synchronization repository.
     *
     * @param jdbc JDBC template
     */
    public JdbcGatewayOpenApiSyncRepository(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public Optional<GatewayOpenApiSyncPO> findByKey(
            GatewayOpenApiSyncKeyDTO key) {
        Objects.requireNonNull(key, "key");
        return jdbc.query(
                        SELECT_COLUMNS + """
                                 WHERE application_id = ?
                                   AND build_id = ?
                                   AND openapi_group = ?
                                """,
                        rowMapper(),
                        key.applicationId(),
                        key.buildId(),
                        key.openapiGroup()
                )
                .stream()
                .findFirst();
    }

    @Override
    public Optional<GatewayOpenApiSyncPO> findById(String syncId) {
        return jdbc.query(
                        SELECT_COLUMNS + " WHERE id = ?",
                        rowMapper(),
                        syncId
                )
                .stream()
                .findFirst();
    }

    @Override
    public GatewayOpenApiSyncPO upsertDiscovered(
            GatewayOpenApiSyncPO state) {
        Objects.requireNonNull(state, "state");
        if (state.status() != GatewayOpenApiSyncStateEnum.DISCOVERED) {
            throw new IllegalArgumentException(
                    "upsertDiscovered requires DISCOVERED state"
            );
        }
        jdbc.update("""
                INSERT INTO gateway_openapi_sync_state(
                    id, application_id, build_id, artifact_version,
                    openapi_group, provider_service_name, provider_group,
                    provider_version, status, latest_snapshot_id,
                    definition_set_id, last_instance_id, attempt_count,
                    last_error_code, last_error_message, first_discovered_at,
                    last_attempt_at, last_success_at, next_retry_at, revision,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                          ?, ?, ?, ?)
                ON CONFLICT (application_id, build_id, openapi_group)
                DO UPDATE SET
                    artifact_version = EXCLUDED.artifact_version,
                    provider_service_name = EXCLUDED.provider_service_name,
                    provider_group = EXCLUDED.provider_group,
                    provider_version = EXCLUDED.provider_version,
                    updated_at = EXCLUDED.updated_at
                """,
                state.id(),
                state.applicationId(),
                state.buildId(),
                state.artifactVersion(),
                state.openapiGroup(),
                state.providerServiceName(),
                state.providerGroup(),
                state.providerVersion(),
                state.status().name(),
                state.latestSnapshotId(),
                state.definitionSetId(),
                state.lastInstanceId(),
                state.attemptCount(),
                state.lastErrorCode(),
                state.lastErrorMessage(),
                timestamp(state.firstDiscoveredAt()),
                optionalTimestamp(state.lastAttemptAt()),
                optionalTimestamp(state.lastSuccessAt()),
                optionalTimestamp(state.nextRetryAt()),
                state.revision(),
                timestamp(state.updatedAt())
        );
        return findByKey(new GatewayOpenApiSyncKeyDTO(
                state.applicationId(),
                state.buildId(),
                state.openapiGroup()
        )).orElseThrow(() -> new IllegalStateException(
                "OpenAPI sync row disappeared after upsert"
        ));
    }

    @Override
    public List<GatewayOpenApiSyncPO> findDue(Instant now, int limit) {
        Objects.requireNonNull(now, "now");
        if (limit <= 0) {
            return List.of();
        }
        return jdbc.query(
                SELECT_COLUMNS + """
                         WHERE status IN """ + CLAIMABLE_STATES + """
                           AND (next_retry_at IS NULL OR next_retry_at <= ?)
                         ORDER BY next_retry_at NULLS FIRST, id
                         LIMIT ?
                        """,
                rowMapper(),
                timestamp(now),
                limit
        );
    }

    @Override
    public boolean claim(String syncId, long expectedRevision, Instant now) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(now, "now");
        int updated = jdbc.update("""
                UPDATE gateway_openapi_sync_state
                   SET status = 'FETCHING',
                       attempt_count = attempt_count + 1,
                       last_attempt_at = ?,
                       next_retry_at = NULL,
                       last_error_code = NULL,
                       last_error_message = NULL,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ?
                   AND revision = ?
                   AND status IN """ + CLAIMABLE_STATES + """
                   AND (next_retry_at IS NULL OR next_retry_at <= ?)
                """,
                timestamp(now),
                timestamp(now),
                syncId,
                expectedRevision,
                timestamp(now)
        );
        return updated == 1;
    }

    @Override
    public boolean transition(
            String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum expectedState,
            GatewayOpenApiSyncStateEnum nextState,
            Instant now) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(expectedState, "expectedState");
        Objects.requireNonNull(nextState, "nextState");
        Objects.requireNonNull(now, "now");
        if (!expectedState.canTransitionTo(nextState)) {
            throw new IllegalArgumentException(
                    "illegal OpenAPI sync transition: "
                            + expectedState
                            + " -> "
                            + nextState
            );
        }
        int updated = jdbc.update("""
                UPDATE gateway_openapi_sync_state
                   SET status = ?, revision = revision + 1, updated_at = ?
                 WHERE id = ? AND revision = ? AND status = ?
                """,
                nextState.name(),
                timestamp(now),
                syncId,
                expectedRevision,
                expectedState.name()
        );
        return updated == 1;
    }

    @Override
    public boolean setValid(
            String syncId,
            long expectedRevision,
            String snapshotId,
            String definitionSetId,
            Instant now) {
        requireRevision(expectedRevision);
        String snapshot = required(snapshotId, "snapshotId");
        String definitionSet = required(definitionSetId, "definitionSetId");
        Objects.requireNonNull(now, "now");
        int updated = jdbc.update("""
                UPDATE gateway_openapi_sync_state
                   SET status = 'VALID',
                       latest_snapshot_id = ?,
                       definition_set_id = ?,
                       last_success_at = ?,
                       next_retry_at = NULL,
                       last_error_code = NULL,
                       last_error_message = NULL,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ? AND revision = ? AND status = 'INGESTING'
                """,
                snapshot,
                definitionSet,
                timestamp(now),
                timestamp(now),
                syncId,
                expectedRevision
        );
        return updated == 1;
    }

    @Override
    public boolean markFailure(
            String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum nextState,
            String errorCode,
            String errorMessage,
            Instant nextRetryAt,
            Instant now) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(nextState, "nextState");
        if (!Set.of(
                GatewayOpenApiSyncStateEnum.INVALID,
                GatewayOpenApiSyncStateEnum.INCONSISTENT_BUILD,
                GatewayOpenApiSyncStateEnum.FETCH_FAILED,
                GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                GatewayOpenApiSyncStateEnum.STALE
        ).contains(nextState)) {
            throw new IllegalArgumentException(
                    "unsupported OpenAPI sync failure state: " + nextState
            );
        }
        String code = required(errorCode, "errorCode");
        String message = required(errorMessage, "errorMessage");
        if (code.length() > 128 || message.length() > 1024) {
            throw new IllegalArgumentException(
                    "OpenAPI sync failure details exceed database limits"
            );
        }
        Objects.requireNonNull(now, "now");
        String allowedStates = allowedFailureStates(nextState);
        int updated = jdbc.update("""
                UPDATE gateway_openapi_sync_state
                   SET status = ?,
                       last_error_code = ?,
                       last_error_message = ?,
                       next_retry_at = ?,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ? AND revision = ? AND status IN """
                + allowedStates,
                nextState.name(),
                code,
                message,
                optionalTimestamp(nextRetryAt),
                timestamp(now),
                syncId,
                expectedRevision
        );
        return updated == 1;
    }

    private static String allowedFailureStates(
            GatewayOpenApiSyncStateEnum nextState) {
        return switch (nextState) {
            case FETCH_FAILED -> "('FETCHING')";
            case INGEST_FAILED -> "('INGESTING')";
            case INVALID, INCONSISTENT_BUILD -> "('VALIDATING')";
            case STALE -> "('DISCOVERED', 'FETCHING', 'VALIDATING', "
                    + "'INVALID', 'INCONSISTENT_BUILD', 'INGESTING', "
                    + "'VALID', 'FETCH_FAILED', 'INGEST_FAILED')";
            default -> throw new IllegalArgumentException(
                    "unsupported OpenAPI sync failure state: " + nextState
            );
        };
    }

    private RowMapper<GatewayOpenApiSyncPO> rowMapper() {
        return (result, row) -> new GatewayOpenApiSyncPO(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("build_id"),
                result.getString("artifact_version"),
                result.getString("openapi_group"),
                result.getString("provider_service_name"),
                result.getString("provider_group"),
                result.getString("provider_version"),
                GatewayOpenApiSyncStateEnum.valueOf(
                        result.getString("status")
                ),
                result.getString("latest_snapshot_id"),
                result.getString("definition_set_id"),
                result.getString("last_instance_id"),
                result.getInt("attempt_count"),
                result.getString("last_error_code"),
                result.getString("last_error_message"),
                instant(result, "first_discovered_at"),
                optionalInstant(result, "last_attempt_at"),
                optionalInstant(result, "last_success_at"),
                optionalInstant(result, "next_retry_at"),
                result.getLong("revision"),
                instant(result, "updated_at")
        );
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(Objects.requireNonNull(instant, "instant"));
    }

    private static Timestamp optionalTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet result, String column)
            throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return Objects.requireNonNull(value, column).toInstant();
    }

    private static Instant optionalInstant(ResultSet result, String column)
            throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static void requireRevision(long revision) {
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
    }
}
