package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.mcp.task.McpTask;
import top.egon.cola.component.gateway.mcp.task.McpTaskStore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * PostgreSQL task store with atomic SKIP LOCKED worker leasing.
 */
public final class JdbcMcpRuntimeTaskStore implements McpTaskStore {

    private static final String COLUMNS = """
            id, principal_fingerprint, subject_id, tenant_id, client_id,
            server_code, tool_name, request_digest, state,
            input_payload::text AS input_payload,
            result_payload::text AS result_payload,
            error_payload::text AS error_payload,
            worker_owner, lease_until, execution_deadline, expires_at,
            attempt_count, max_attempts, revision, created_at, updated_at
            """;

    private final DataSource dataSource;

    private final ObjectMapper objectMapper;

    public JdbcMcpRuntimeTaskStore(
            DataSource dataSource,
            ObjectMapper objectMapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    @Override
    public Mono<Void> create(McpTask task) {
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
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
                         """)) {
                bindTask(statement, task);
                statement.executeUpdate();
                return Boolean.TRUE;
            }
        }).then();
    }

    @Override
    public Mono<McpTask> find(String taskId) {
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return find(connection, taskId);
            }
        }).flatMap(task -> task == null ? Mono.empty() : Mono.just(task));
    }

    @Override
    public Mono<McpTask> leaseNext(
            String workerOwner,
            Instant now,
            Instant leaseUntil) {
        return blocking(() -> leaseBlocking(
                required(workerOwner, "workerOwner"),
                Objects.requireNonNull(now, "now"),
                Objects.requireNonNull(leaseUntil, "leaseUntil")
        )).flatMap(task -> task == null ? Mono.empty() : Mono.just(task));
    }

    @Override
    public Mono<Boolean> renewLease(
            String taskId,
            String workerOwner,
            Instant now,
            Instant leaseUntil) {
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE gateway_mcp_task_instance
                            SET lease_until = ?, updated_at = ?
                          WHERE id = ? AND state = 'WORKING'
                            AND worker_owner = ? AND lease_until > ?
                         """)) {
                statement.setTimestamp(1, timestamp(leaseUntil));
                statement.setTimestamp(2, timestamp(now));
                statement.setString(3, required(taskId, "taskId"));
                statement.setString(4, required(workerOwner, "workerOwner"));
                statement.setTimestamp(5, timestamp(now));
                return statement.executeUpdate() == 1;
            }
        });
    }

    @Override
    public Mono<Boolean> transition(Transition transition) {
        Objects.requireNonNull(transition, "transition");
        return blocking(() -> {
            String workerClause = transition.expectedWorkerOwner() == null
                    ? "worker_owner IS NULL"
                    : "worker_owner = ?";
            String sql = """
                    UPDATE gateway_mcp_task_instance
                       SET state = ?, input_payload = ?::jsonb,
                           result_payload = ?::jsonb,
                           error_payload = ?::jsonb,
                           worker_owner = NULL, lease_until = NULL,
                           revision = revision + 1, updated_at = ?
                     WHERE id = ? AND state = ? AND revision = ?
                       AND %s
                    """.formatted(workerClause);
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         sql
                 )) {
                int index = 1;
                statement.setString(index++, transition.targetState().name());
                statement.setString(index++, json(transition.inputPayload()));
                statement.setString(index++, json(transition.resultPayload()));
                statement.setString(index++, json(transition.errorPayload()));
                statement.setTimestamp(index++, timestamp(transition.now()));
                statement.setString(index++, transition.taskId());
                statement.setString(index++, transition.expectedState().name());
                statement.setLong(index++, transition.expectedRevision());
                if (transition.expectedWorkerOwner() != null) {
                    statement.setString(
                            index,
                            transition.expectedWorkerOwner()
                    );
                }
                return statement.executeUpdate() == 1;
            }
        });
    }

    @Override
    public Mono<Boolean> cancel(
            String taskId,
            McpTask.State expectedState,
            long expectedRevision,
            Instant now) {
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE gateway_mcp_task_instance
                            SET state = 'CANCELLED', worker_owner = NULL,
                                lease_until = NULL, revision = revision + 1,
                                updated_at = ?
                          WHERE id = ? AND state = ? AND revision = ?
                         """)) {
                statement.setTimestamp(1, timestamp(now));
                statement.setString(2, taskId);
                statement.setString(3, expectedState.name());
                statement.setLong(4, expectedRevision);
                return statement.executeUpdate() == 1;
            }
        });
    }

    @Override
    public Mono<Integer> failUnavailable(Instant now) {
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE gateway_mcp_task_instance
                            SET state = 'FAILED',
                                error_payload =
                                    '{"code":"MCP_TASK_UNAVAILABLE"}'::jsonb,
                                worker_owner = NULL, lease_until = NULL,
                                revision = revision + 1, updated_at = ?
                          WHERE state = 'WORKING'
                            AND (
                                execution_deadline <= ?
                                OR (
                                    attempt_count >= max_attempts
                                    AND (lease_until IS NULL OR lease_until <= ?)
                                )
                            )
                         """)) {
                Timestamp timestamp = timestamp(now);
                statement.setTimestamp(1, timestamp);
                statement.setTimestamp(2, timestamp);
                statement.setTimestamp(3, timestamp);
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public Mono<Integer> deleteExpired(Instant now) {
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         DELETE FROM gateway_mcp_task_instance
                          WHERE state IN ('COMPLETED', 'FAILED', 'CANCELLED')
                            AND expires_at <= ?
                         """)) {
                statement.setTimestamp(1, timestamp(now));
                return statement.executeUpdate();
            }
        });
    }

    private McpTask leaseBlocking(
            String workerOwner,
            Instant now,
            Instant leaseUntil) throws SQLException {
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String taskId;
                try (PreparedStatement select = connection.prepareStatement("""
                        SELECT id
                          FROM gateway_mcp_task_instance
                         WHERE state = 'WORKING'
                           AND expires_at > ?
                           AND execution_deadline > ?
                           AND attempt_count < max_attempts
                           AND (worker_owner IS NULL OR lease_until <= ?)
                         ORDER BY created_at, id
                         LIMIT 1
                         FOR UPDATE SKIP LOCKED
                        """)) {
                    Timestamp timestamp = timestamp(now);
                    select.setTimestamp(1, timestamp);
                    select.setTimestamp(2, timestamp);
                    select.setTimestamp(3, timestamp);
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            return null;
                        }
                        taskId = result.getString(1);
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE gateway_mcp_task_instance
                           SET worker_owner = ?, lease_until = ?,
                               attempt_count = attempt_count + 1,
                               revision = revision + 1, updated_at = ?
                         WHERE id = ?
                        """)) {
                    update.setString(1, workerOwner);
                    update.setTimestamp(2, timestamp(leaseUntil));
                    update.setTimestamp(3, timestamp(now));
                    update.setString(4, taskId);
                    update.executeUpdate();
                }
                McpTask task = find(connection, taskId);
                connection.commit();
                return task;
            } catch (RuntimeException | SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private McpTask find(Connection connection, String taskId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + COLUMNS
                        + " FROM gateway_mcp_task_instance WHERE id = ?"
        )) {
            statement.setString(1, required(taskId, "taskId"));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    private McpTask map(ResultSet result) throws SQLException {
        return new McpTask(
                result.getString("id"),
                result.getString("principal_fingerprint"),
                result.getString("subject_id"),
                result.getString("tenant_id"),
                result.getString("client_id"),
                result.getString("server_code"),
                result.getString("tool_name"),
                result.getString("request_digest"),
                McpTask.State.valueOf(result.getString("state")),
                map(result.getString("input_payload")),
                map(result.getString("result_payload")),
                map(result.getString("error_payload")),
                result.getString("worker_owner"),
                instant(result.getTimestamp("lease_until")),
                result.getTimestamp("execution_deadline").toInstant(),
                result.getTimestamp("expires_at").toInstant(),
                result.getInt("attempt_count"),
                result.getInt("max_attempts"),
                result.getLong("revision"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    private void bindTask(PreparedStatement statement, McpTask task)
            throws SQLException {
        int index = 1;
        statement.setString(index++, task.id());
        statement.setString(index++, task.principalFingerprint());
        statement.setString(index++, task.subjectId());
        statement.setString(index++, task.tenantId());
        statement.setString(index++, task.clientId());
        statement.setString(index++, task.serverCode());
        statement.setString(index++, task.toolName());
        statement.setString(index++, task.requestDigest());
        statement.setString(index++, task.state().name());
        statement.setString(index++, json(task.inputPayload()));
        statement.setString(index++, json(task.resultPayload()));
        statement.setString(index++, json(task.errorPayload()));
        statement.setString(index++, task.workerOwner());
        statement.setTimestamp(index++, timestamp(task.leaseUntil()));
        statement.setTimestamp(index++, timestamp(task.executionDeadline()));
        statement.setTimestamp(index++, timestamp(task.expiresAt()));
        statement.setInt(index++, task.attemptCount());
        statement.setInt(index++, task.maxAttempts());
        statement.setLong(index++, task.revision());
        statement.setTimestamp(index++, timestamp(task.createdAt()));
        statement.setTimestamp(index, timestamp(task.updatedAt()));
    }

    private Map<String, Object> map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "MCP task payload is invalid",
                    failure
            );
        }
    }

    private String json(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "MCP task payload cannot be serialized",
                    failure
            );
        }
    }

    private <T> Mono<T> blocking(CheckedSupplier<T> supplier) {
        return Mono.fromCallable(supplier::get)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {

        T get() throws Exception;
    }
}
