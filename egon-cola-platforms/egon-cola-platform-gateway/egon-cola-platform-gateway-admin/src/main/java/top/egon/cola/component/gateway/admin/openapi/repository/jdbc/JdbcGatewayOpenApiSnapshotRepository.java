package top.egon.cola.component.gateway.admin.openapi.repository.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.gateway.admin.openapi.repository.GatewayOpenApiSnapshotRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JDBC persistence adapter for immutable OpenAPI Group snapshots.
 *
 * <p>中文：使用 JdbcTemplate 保存不可变 OpenAPI 快照，并以受影响行数实现
 * Definition Set 链接的幂等与冲突检测。
 */
@Repository("gatewayOpenApiSnapshotRepository")
public class JdbcGatewayOpenApiSnapshotRepository
        implements GatewayOpenApiSnapshotRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, application_id, definition_set_id, build_id,
                   artifact_version, openapi_group, openapi_version,
                   document_sha256, canonical_sha256,
                   document_json::text AS document_json,
                   validation_status, validation_messages::text
                       AS validation_messages,
                   operation_count, schema_count, fetched_from_instance_id,
                   fetched_at, validated_at, created_at
              FROM gateway_openapi_snapshot
            """;

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    /**
     * Creates the snapshot repository.
     *
     * @param jdbc JDBC template
     * @param objectMapper Spring-managed Jackson mapper
     */
    public JdbcGatewayOpenApiSnapshotRepository(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    @Override
    public Optional<GatewayOpenApiSnapshotPO> findById(String snapshotId) {
        return jdbc.query(
                        SELECT_COLUMNS + " WHERE id = ?",
                        rowMapper(),
                        snapshotId
                )
                .stream()
                .findFirst();
    }

    @Override
    public Optional<GatewayOpenApiSnapshotPO> findByContract(
            String applicationId,
            String buildId,
            String openapiGroup,
            String canonicalSha256) {
        return jdbc.query(
                        SELECT_COLUMNS + """
                                 WHERE application_id = ?
                                   AND build_id = ?
                                   AND openapi_group = ?
                                   AND canonical_sha256 = ?
                                """,
                        rowMapper(),
                        applicationId,
                        buildId,
                        openapiGroup,
                        canonicalSha256
                )
                .stream()
                .findFirst();
    }

    @Override
    public Optional<GatewayOpenApiSnapshotPO>
    findByApplicationGroupAndCanonicalSha256(
            String applicationId,
            String openapiGroup,
            String canonicalSha256) {
        return jdbc.query(
                        SELECT_COLUMNS + """
                                 WHERE application_id = ?
                                   AND openapi_group = ?
                                   AND canonical_sha256 = ?
                                 ORDER BY fetched_at DESC, id
                                """,
                        rowMapper(),
                        applicationId,
                        openapiGroup,
                        canonicalSha256
                )
                .stream()
                .findFirst();
    }

    @Override
    public GatewayOpenApiSnapshotPO insertOrReuse(
            GatewayOpenApiSnapshotPO snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        int inserted = jdbc.update("""
                INSERT INTO gateway_openapi_snapshot(
                    id, application_id, definition_set_id, build_id,
                    artifact_version, openapi_group, openapi_version,
                    document_sha256, canonical_sha256, document_json,
                    validation_status, validation_messages, operation_count,
                    schema_count, fetched_from_instance_id, fetched_at,
                    validated_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb,
                          ?, ?, ?, ?, ?, ?)
                ON CONFLICT (
                    application_id, build_id, openapi_group, canonical_sha256
                ) DO NOTHING
                """,
                snapshot.id(),
                snapshot.applicationId(),
                snapshot.definitionSetId(),
                snapshot.buildId(),
                snapshot.artifactVersion(),
                snapshot.openapiGroup(),
                snapshot.openapiVersion(),
                snapshot.documentSha256(),
                snapshot.canonicalSha256(),
                json(snapshot.documentJson()),
                snapshot.validationStatus(),
                json(snapshot.validationMessages()),
                snapshot.operationCount(),
                snapshot.schemaCount(),
                snapshot.fetchedFromInstanceId(),
                timestamp(snapshot.fetchedAt()),
                timestamp(snapshot.validatedAt()),
                timestamp(snapshot.createdAt())
        );
        if (inserted == 1) {
            return snapshot;
        }
        GatewayOpenApiSnapshotPO existing = findByContract(
                snapshot.applicationId(),
                snapshot.buildId(),
                snapshot.openapiGroup(),
                snapshot.canonicalSha256()
        ).orElseThrow(() -> new IllegalStateException(
                "GATEWAY_OPENAPI_SNAPSHOT_CONFLICT: snapshot identity "
                        + snapshot.id()
                        + " could not be resolved"
        ));
        if (!sameContract(existing, snapshot)) {
            throw new IllegalStateException(
                    "GATEWAY_OPENAPI_SNAPSHOT_CONFLICT: immutable contract "
                            + snapshot.applicationId()
                            + "/"
                            + snapshot.buildId()
                            + "/"
                            + snapshot.openapiGroup()
            );
        }
        return existing;
    }

    @Override
    public List<GatewayOpenApiSnapshotPO> findByBuildGroups(
            String applicationId,
            String buildId,
            List<String> openapiGroups) {
        Objects.requireNonNull(openapiGroups, "openapiGroups");
        Set<String> groups = openapiGroups.stream()
                .map(value -> Objects.requireNonNull(value, "openapiGroup"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (groups.isEmpty()) {
            return List.of();
        }
        String placeholders = groups.stream()
                .map(ignored -> "?")
                .collect(Collectors.joining(", "));
        List<Object> arguments = new ArrayList<>();
        arguments.add(applicationId);
        arguments.add(buildId);
        arguments.addAll(groups);
        return jdbc.query(
                SELECT_COLUMNS + " WHERE application_id = ?"
                        + " AND build_id = ?"
                        + " AND openapi_group IN (" + placeholders + ")"
                        + " ORDER BY openapi_group, id",
                rowMapper(),
                arguments.toArray()
        );
    }

    @Override
    public int linkAllToDefinitionSet(
            List<String> snapshotIds,
            String definitionSetId) {
        Objects.requireNonNull(snapshotIds, "snapshotIds");
        String setId = required(definitionSetId, "definitionSetId");
        Set<String> ids = snapshotIds.stream()
                .map(value -> required(value, "snapshotId"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int linked = 0;
        for (String snapshotId : ids) {
            int updated = jdbc.update("""
                    UPDATE gateway_openapi_snapshot
                       SET definition_set_id = ?
                     WHERE id = ? AND definition_set_id IS NULL
                    """, setId, snapshotId);
            if (updated == 1) {
                linked++;
                continue;
            }
            String existing = jdbc.query(
                            "SELECT definition_set_id"
                                    + " FROM gateway_openapi_snapshot"
                                    + " WHERE id = ?",
                            (result, row) -> result.getString(1),
                            snapshotId
                    )
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "GATEWAY_OPENAPI_SNAPSHOT_NOT_FOUND: "
                                    + snapshotId
                    ));
            if (!setId.equals(existing)) {
                throw new IllegalStateException(
                        "GATEWAY_OPENAPI_SNAPSHOT_CONFLICT: snapshot "
                                + snapshotId
                                + " already links to another definition set"
                );
            }
            linked++;
        }
        return linked;
    }

    @Override
    public List<GatewayOpenApiSnapshotPO> findByDefinitionSetId(
            String definitionSetId) {
        return jdbc.query(
                SELECT_COLUMNS + """
                         WHERE definition_set_id = ?
                         ORDER BY openapi_group, id
                        """,
                rowMapper(),
                definitionSetId
        );
    }

    private boolean sameContract(
            GatewayOpenApiSnapshotPO left,
            GatewayOpenApiSnapshotPO right) {
        return left.artifactVersion().equals(right.artifactVersion())
                && left.openapiVersion().equals(right.openapiVersion())
                && left.documentJson().equals(right.documentJson())
                && left.validationStatus().equals(right.validationStatus())
                && left.validationMessages().equals(
                right.validationMessages()
        )
                && left.operationCount() == right.operationCount()
                && left.schemaCount() == right.schemaCount();
    }

    private RowMapper<GatewayOpenApiSnapshotPO> rowMapper() {
        return (result, row) -> new GatewayOpenApiSnapshotPO(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("definition_set_id"),
                result.getString("build_id"),
                result.getString("artifact_version"),
                result.getString("openapi_group"),
                result.getString("openapi_version"),
                result.getString("document_sha256"),
                result.getString("canonical_sha256"),
                readMap(result, "document_json"),
                result.getString("validation_status"),
                readMessages(result, "validation_messages"),
                result.getInt("operation_count"),
                result.getInt("schema_count"),
                result.getString("fetched_from_instance_id"),
                instant(result, "fetched_at"),
                instant(result, "validated_at"),
                instant(result, "created_at")
        );
    }

    private Map<String, Object> readMap(ResultSet result, String column)
            throws SQLException {
        try {
            return objectMapper.readValue(
                    result.getString(column),
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "invalid JSON object in " + column,
                    failure
            );
        }
    }

    private List<String> readMessages(ResultSet result, String column)
            throws SQLException {
        try {
            return objectMapper.readValue(
                    result.getString(column),
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "invalid JSON array in " + column,
                    failure
            );
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "OpenAPI snapshot JSON cannot be serialized",
                    failure
            );
        }
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(Objects.requireNonNull(instant, "instant"));
    }

    private static Instant instant(ResultSet result, String column)
            throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return Objects.requireNonNull(value, column).toInstant();
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
