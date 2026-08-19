package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.mcp.task.domain.McpTask;
import top.egon.cola.component.gateway.mcp.task.service.McpTaskStore;

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
 * 补充说明 / Supplementary summary: {@code JdbcMcpRuntimeTaskStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP运行时任务存储相关的职责与边界。
 * English supplement: {@code JdbcMcpRuntimeTaskStore} is a jdbc mcp runtime task store store in the current Gateway module; it owns the jdbc mcp runtime task store-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class JdbcMcpRuntimeTaskStore implements McpTaskStore {

    /**
     * 中文说明：表示 COLUMNS 这一固定值；它属于 {@code JdbcMcpRuntimeTaskStore} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value columns; it is a state, type, or protocol value of {@code JdbcMcpRuntimeTaskStore} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRuntimeTaskStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRuntimeTaskStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String COLUMNS = """
            id, principal_fingerprint, subject_id, tenant_id, client_id,
            server_code, tool_name, request_digest, state,
            input_payload::text AS input_payload,
            result_payload::text AS result_payload,
            error_payload::text AS error_payload,
            worker_owner, lease_until, execution_deadline, expires_at,
            attempt_count, max_attempts, revision, created_at, updated_at
            """;

    /**
     * 中文说明：保存 dataSource 对应的状态、依赖或配置值；字段类型为 {@code DataSource}，由 {@code JdbcMcpRuntimeTaskStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by data source; its type is {@code DataSource}, and {@code JdbcMcpRuntimeTaskStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRuntimeTaskStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRuntimeTaskStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DataSource dataSource;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code JdbcMcpRuntimeTaskStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code JdbcMcpRuntimeTaskStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRuntimeTaskStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRuntimeTaskStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code JdbcMcpRuntimeTaskStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpRuntimeTaskStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param dataSource 参数 dataSource；parameter data source。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpRuntimeTaskStore(
            DataSource dataSource,
            ObjectMapper objectMapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<McpTask> find(String taskId) {
        return blocking(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return find(connection, taskId);
            }
        }).flatMap(task -> task == null ? Mono.empty() : Mono.just(task));
    }

    /**
     * 中文说明：执行 租约Next 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the lease next operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.leaseNext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param now 参数 now；parameter now。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @return 返回 租约Next 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 renew租约 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the renew lease operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.renewLease(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param now 参数 now；parameter now。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @return 返回 renew租约 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 transition 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.transition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param transition 参数 transition；parameter transition。
     * @return 返回 transition 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 cancel 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param expectedState 参数 expectedState；parameter expected state。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param now 参数 now；parameter now。
     * @return 返回 cancel 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 failUnavailable 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the fail unavailable operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.failUnavailable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 failUnavailable 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 租约Blocking 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the lease blocking operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.leaseBlocking(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param now 参数 now；parameter now。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @return 返回 租约Blocking 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param connection 参数 connection；parameter connection。
     * @param taskId 参数 任务Id；parameter task id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 bind任务 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bind task operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.bindTask(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param statement 参数 statement；parameter statement。
     * @param task 参数 任务；parameter task。
     */
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

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 json 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 blocking 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the blocking operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.blocking(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param supplier 参数 supplier；parameter supplier。
     * @return 返回 blocking 的处理结果；returns the result of the operation.
     */
    private <T> Mono<T> blocking(CheckedSupplier<T> supplier) {
        return Mono.fromCallable(supplier::get)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 中文说明：执行 timestamp 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timestamp operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.timestamp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 timestamp 的处理结果；returns the result of the operation.
     */
    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    /**
     * 中文说明：执行 instant 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instant operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.instant(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 instant 的处理结果；returns the result of the operation.
     */
    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code JdbcMcpRuntimeTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：{@code CheckedSupplier} 是接口契约，位于当前 Gateway 模块的相关包中，负责CheckedSupplier相关的职责与边界。
     * English summary: {@code CheckedSupplier} is an interface contract in the current Gateway module; it owns the checked supplier-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    private interface CheckedSupplier<T> {

        /**
         * 中文说明：执行 get 操作；该方法是 {@code JdbcMcpRuntimeTaskStore.CheckedSupplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get operation; this method is the invocation entry point on {@code JdbcMcpRuntimeTaskStore.CheckedSupplier} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRuntimeTaskStore.CheckedSupplier.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get 的处理结果；returns the result of the operation.
         */
        T get() throws Exception;
    }
}
