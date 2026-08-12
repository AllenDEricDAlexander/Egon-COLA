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

/**
 * 中文说明：{@code JdbcMcpTaskStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP任务存储相关的职责与边界。
 * English summary: {@code JdbcMcpTaskStore} is a jdbc mcp task store store in the current Gateway module; it owns the jdbc mcp task store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpTaskStore {

    /**
     * 中文说明：表示 STATES 这一固定值；它属于 {@code JdbcMcpTaskStore} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value states; it is a state, type, or protocol value of {@code JdbcMcpTaskStore} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> STATES = Set.of(
            "WORKING",
            "INPUT_REQUIRED",
            "COMPLETED",
            "FAILED",
            "CANCELLED"
    );

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpTaskStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpTaskStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpTaskStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpTaskStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpTaskStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpTaskStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpTaskStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     */
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

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 list 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    public List<TaskRecord> list(String tenantId, String clientId) {
        return jdbc.query("""
                SELECT id, principal_fingerprint, subject_id, tenant_id,
                       client_id, server_code, tool_name, request_digest,
                       state, input_payload::text AS input_payload,
                       result_payload::text AS result_payload,
                       error_payload::text AS error_payload,
                       worker_owner, lease_until, execution_deadline,
                       expires_at, attempt_count, max_attempts, revision,
                       created_at, updated_at
                  FROM gateway_mcp_task_instance
                 WHERE tenant_id = ? AND (? IS NULL OR client_id = ?)
                 ORDER BY created_at DESC
                 LIMIT 500
                """, (result, row) -> map(result),
                tenantId,
                clientId,
                clientId
        );
    }

    /**
     * 中文说明：执行 claim 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the claim operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.claim(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param now 参数 now；parameter now。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 claim 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 transition 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.transition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param currentState 参数 currentState；parameter current state。
     * @param targetState 参数 targetState；parameter target state。
     * @param resultPayload 参数 resultPayload；parameter result payload。
     * @param errorPayload 参数 errorPayload；parameter error payload。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param now 参数 now；parameter now。
     * @return 返回 transition 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 cancel 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param now 参数 now；parameter now。
     * @return 返回 cancel 的处理结果；returns the result of the operation.
     */
    public boolean cancel(
            String id,
            long expectedRevision,
            Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_task_instance
                   SET state = 'CANCELLED', worker_owner = NULL,
                       lease_until = NULL, revision = revision + 1,
                       updated_at = ?
                 WHERE id = ? AND revision = ?
                   AND state IN ('WORKING', 'INPUT_REQUIRED')
                """,
                McpJdbcJson.timestamp(now),
                id,
                expectedRevision
        ) == 1;
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private TaskRecord map(java.sql.ResultSet result)
            throws java.sql.SQLException {
        return new TaskRecord(
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
        );
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> read(String value) {
        return value == null ? null : json.map(value);
    }

    /**
     * 中文说明：执行 write 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.write(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 write 的处理结果；returns the result of the operation.
     */
    private String write(Map<String, Object> value) {
        return value == null ? null : json.write(value);
    }

    /**
     * 中文说明：执行 instant 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instant operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.instant(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 instant 的处理结果；returns the result of the operation.
     */
    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /**
     * 中文说明：执行 state 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the state operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.state(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 state 的处理结果；returns the result of the operation.
     */
    private static String state(String value) {
        String candidate = McpJdbcJson.required(value, "state");
        if (!STATES.contains(candidate)) {
            throw new IllegalArgumentException("unsupported task state");
        }
        return candidate;
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code JdbcMcpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code JdbcMcpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 digest 的处理结果；returns the result of the operation.
     */
    private static String digest(String value, String field) {
        String digest = McpJdbcJson.required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }

    /**
     * 中文说明：{@code TaskRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责任务Record相关的职责与边界。
     * English summary: {@code TaskRecord} is an immutable data carrier in the current Gateway module; it owns the task record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param principalFingerprint 参数 principalFingerprint；parameter principal fingerprint。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param requestDigest 参数 请求Digest；parameter request digest。
     * @param state 参数 state；parameter state。
     * @param inputPayload 参数 inputPayload；parameter input payload。
     * @param resultPayload 参数 resultPayload；parameter result payload。
     * @param errorPayload 参数 errorPayload；parameter error payload。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @param executionDeadline 参数 executionDeadline；parameter execution deadline。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @param attemptCount 参数 attemptCount；parameter attempt count。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     * @param revision 参数 revision；parameter revision。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    public record TaskRecord(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 principalFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by principal fingerprint; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String principalFingerprint,
            /**
             * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectId,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String toolName,
            /**
             * 中文说明：保存 请求Digest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request digest; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String requestDigest,
            /**
             * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String state,
            /**
             * 中文说明：保存 inputPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input payload; its type is {@code Map<String, Object>}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> inputPayload,
            /**
             * 中文说明：保存 resultPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by result payload; its type is {@code Map<String, Object>}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> resultPayload,
            /**
             * 中文说明：保存 errorPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error payload; its type is {@code Map<String, Object>}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> errorPayload,
            /**
             * 中文说明：保存 workerOwner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by worker owner; its type is {@code String}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String workerOwner,
            /**
             * 中文说明：保存 租约Until 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lease until; its type is {@code Instant}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant leaseUntil,
            /**
             * 中文说明：保存 executionDeadline 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by execution deadline; its type is {@code Instant}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant executionDeadline,
            /**
             * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expiresAt,
            /**
             * 中文说明：保存 attemptCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attempt count; its type is {@code int}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            int attemptCount,
            /**
             * 中文说明：保存 maxAttempts 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by max attempts; its type is {@code int}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            int maxAttempts,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpTaskStore.TaskRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code JdbcMcpTaskStore.TaskRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskStore.TaskRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskStore.TaskRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpTaskStore.TaskRecord} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpTaskStore.TaskRecord} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param principalFingerprint 参数 principalFingerprint；parameter principal fingerprint。
         * @param subjectId 参数 subjectId；parameter subject id。
         * @param tenantId 参数 tenantId；parameter tenant id。
         * @param clientId 参数 客户端Id；parameter client id。
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param toolName 参数 工具Name；parameter tool name。
         * @param requestDigest 参数 请求Digest；parameter request digest。
         * @param state 参数 state；parameter state。
         * @param inputPayload 参数 inputPayload；parameter input payload。
         * @param resultPayload 参数 resultPayload；parameter result payload。
         * @param errorPayload 参数 errorPayload；parameter error payload。
         * @param workerOwner 参数 workerOwner；parameter worker owner。
         * @param leaseUntil 参数 租约Until；parameter lease until。
         * @param executionDeadline 参数 executionDeadline；parameter execution deadline。
         * @param expiresAt 参数 expiresAt；parameter expires at。
         * @param attemptCount 参数 attemptCount；parameter attempt count。
         * @param maxAttempts 参数 maxAttempts；parameter max attempts。
         * @param revision 参数 revision；parameter revision。
         * @param createdAt 参数 createdAt；parameter created at。
         * @param updatedAt 参数 updatedAt；parameter updated at。
         */
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

        /**
         * 中文说明：执行 copy 操作；该方法是 {@code JdbcMcpTaskStore.TaskRecord} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the copy operation; this method is the invocation entry point on {@code JdbcMcpTaskStore.TaskRecord} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.TaskRecord.copy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @return 返回 copy 的处理结果；returns the result of the operation.
         */
        private static Map<String, Object> copy(Map<String, Object> value) {
            return value == null ? null : Map.copyOf(value);
        }

        /**
         * 中文说明：执行 optional 操作；该方法是 {@code JdbcMcpTaskStore.TaskRecord} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the optional operation; this method is the invocation entry point on {@code JdbcMcpTaskStore.TaskRecord} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskStore.TaskRecord.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @return 返回 optional 的处理结果；returns the result of the operation.
         */
        private static String optional(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
