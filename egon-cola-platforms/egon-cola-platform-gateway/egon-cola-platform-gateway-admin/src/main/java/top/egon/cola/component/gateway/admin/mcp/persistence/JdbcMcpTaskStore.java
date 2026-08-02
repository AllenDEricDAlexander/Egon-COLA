package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
public class JdbcMcpTaskStore {

    private static final Set<String> STATES = Set.of(
            "WORKING",
            "INPUT_REQUIRED",
            "COMPLETED",
            "FAILED",
            "CANCELLED"
    );

    private final JdbcTemplate jdbc;

    private final McpJdbcJson json;

    public JdbcMcpTaskStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    public void create(TaskRecord task) {
        Objects.requireNonNull(task, "task");
        jdbc.update("""
                INSERT INTO gateway_mcp_task_instance(
                    id, principal_fingerprint, subject_id, tenant_id,
                    client_id, server_code, tool_name, request_digest,
                    state, input_payload, result_payload, error_payload,
                    worker_owner, lease_until, execution_deadline,
                    expires_at, attempt_count, max_attempts, revision,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb,
                    ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                task.id(),
                task.principalFingerprint(),
                task.subjectId(),
                task.tenantId(),
                task.clientId(),
                task.serverCode(),
                task.toolName(),
                task.requestDigest(),
                state(task.state()),
                write(task.inputPayload()),
                write(task.resultPayload()),
                write(task.errorPayload()),
                task.workerOwner(),
                McpJdbcJson.timestamp(task.leaseUntil()),
                McpJdbcJson.timestamp(task.executionDeadline()),
                McpJdbcJson.timestamp(task.expiresAt()),
                task.attemptCount(),
                task.maxAttempts(),
                task.revision(),
                McpJdbcJson.timestamp(task.createdAt()),
                McpJdbcJson.timestamp(task.updatedAt())
        );
    }

    public Optional<TaskRecord> find(String id) {
        List<TaskRecord> values = jdbc.query("""
                SELECT id, principal_fingerprint, subject_id, tenant_id,
                       client_id, server_code, tool_name, request_digest,
                       state, input_payload::text AS input_payload,
                       result_payload::text AS result_payload,
                       error_payload::text AS error_payload,
                       worker_owner, lease_until, execution_deadline,
                       expires_at, attempt_count, max_attempts, revision,
                       created_at, updated_at
                  FROM gateway_mcp_task_instance
                 WHERE id = ?
                """, (result, row) -> new TaskRecord(
                result.getString("id"),
                result.getString("principal_fingerprint"),
                result.getString("subject_id"),
                result.getString("tenant_id"),
                result.getString("client_id"),
                result.getString("server_code"),
                result.getString("tool_name"),
                result.getString("request_digest"),
                result.getString("state"),
                read(result.getString("input_payload")),
                read(result.getString("result_payload")),
                read(result.getString("error_payload")),
                result.getString("worker_owner"),
                instant(result.getTimestamp("lease_until")),
                result.getTimestamp("execution_deadline").toInstant(),
                result.getTimestamp("expires_at").toInstant(),
                result.getInt("attempt_count"),
                result.getInt("max_attempts"),
                result.getLong("revision"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        ), McpJdbcJson.required(id, "id"));
        return values.stream().findFirst();
    }

    public boolean claim(
            String id,
            String workerOwner,
            Instant now,
            Instant leaseUntil,
            long expectedRevision) {
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        return jdbc.update("""
                UPDATE gateway_mcp_task_instance
                   SET worker_owner = ?, lease_until = ?,
                       attempt_count = attempt_count + 1,
                       revision = revision + 1, updated_at = ?
                 WHERE id = ?
                   AND revision = ?
                   AND state = 'WORKING'
                   AND expires_at > ?
                   AND execution_deadline > ?
                   AND attempt_count < max_attempts
                   AND (worker_owner IS NULL OR lease_until <= ?)
                """,
                McpJdbcJson.required(workerOwner, "workerOwner"),
                McpJdbcJson.timestamp(leaseUntil),
                McpJdbcJson.timestamp(now),
                McpJdbcJson.required(id, "id"),
                expectedRevision,
                McpJdbcJson.timestamp(now),
                McpJdbcJson.timestamp(now),
                McpJdbcJson.timestamp(now)
        ) == 1;
    }

    public boolean transition(
            String id,
            String currentState,
            String targetState,
            Map<String, Object> resultPayload,
            Map<String, Object> errorPayload,
            long expectedRevision,
            Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_task_instance
                   SET state = ?, result_payload = ?::jsonb,
                       error_payload = ?::jsonb,
                       worker_owner = NULL, lease_until = NULL,
                       revision = revision + 1, updated_at = ?
                 WHERE id = ? AND state = ? AND revision = ?
                """,
                state(targetState),
                write(resultPayload),
                write(errorPayload),
                McpJdbcJson.timestamp(now),
                McpJdbcJson.required(id, "id"),
                state(currentState),
                expectedRevision
        ) == 1;
    }

    private Map<String, Object> read(String value) {
        return value == null ? null : json.map(value);
    }

    private String write(Map<String, Object> value) {
        return value == null ? null : json.write(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String state(String value) {
        String candidate = McpJdbcJson.required(value, "state");
        if (!STATES.contains(candidate)) {
            throw new IllegalArgumentException("unsupported task state");
        }
        return candidate;
    }

    private static String digest(String value, String field) {
        String digest = McpJdbcJson.required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }

    public record TaskRecord(
            String id,
            String principalFingerprint,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String requestDigest,
            String state,
            Map<String, Object> inputPayload,
            Map<String, Object> resultPayload,
            Map<String, Object> errorPayload,
            String workerOwner,
            Instant leaseUntil,
            Instant executionDeadline,
            Instant expiresAt,
            int attemptCount,
            int maxAttempts,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {

        public TaskRecord {
            id = McpJdbcJson.required(id, "id");
            principalFingerprint = McpJdbcJson.required(
                    principalFingerprint,
                    "principalFingerprint"
            );
            subjectId = McpJdbcJson.required(subjectId, "subjectId");
            tenantId = McpJdbcJson.required(tenantId, "tenantId");
            clientId = McpJdbcJson.required(clientId, "clientId");
            serverCode = McpJdbcJson.required(serverCode, "serverCode");
            toolName = McpJdbcJson.required(toolName, "toolName");
            requestDigest = digest(requestDigest, "requestDigest");
            state = JdbcMcpTaskStore.state(state);
            inputPayload = copy(inputPayload);
            resultPayload = copy(resultPayload);
            errorPayload = copy(errorPayload);
            workerOwner = optional(workerOwner);
            executionDeadline = Objects.requireNonNull(
                    executionDeadline,
                    "executionDeadline"
            );
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
            updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
            if ((workerOwner == null) != (leaseUntil == null)) {
                throw new IllegalArgumentException(
                        "workerOwner and leaseUntil must be set together"
                );
            }
            if (!executionDeadline.isAfter(createdAt)
                    || !expiresAt.isAfter(createdAt)) {
                throw new IllegalArgumentException(
                        "task deadlines must be after createdAt"
                );
            }
            if (attemptCount < 0 || maxAttempts <= 0
                    || attemptCount > maxAttempts || revision < 0) {
                throw new IllegalArgumentException(
                        "task attempts and revision are invalid"
                );
            }
        }

        private static Map<String, Object> copy(Map<String, Object> value) {
            return value == null ? null : Map.copyOf(value);
        }

        private static String optional(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
