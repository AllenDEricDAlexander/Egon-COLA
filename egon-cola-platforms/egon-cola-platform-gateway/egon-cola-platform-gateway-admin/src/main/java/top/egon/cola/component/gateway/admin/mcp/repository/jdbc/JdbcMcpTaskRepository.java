package top.egon.cola.component.gateway.admin.mcp.repository.jdbc;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
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


import top.egon.cola.component.gateway.admin.mcp.domain.po.McpTaskPO;
/**
 * 中文说明：{@code JdbcMcpTaskRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP任务存储相关的职责与边界。
 * English summary: {@code JdbcMcpTaskRepository} is a jdbc mcp task store store in the current Gateway module; it owns the jdbc mcp task store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpTaskRepository {

    /**
     * 中文说明：表示 STATES 这一固定值；它属于 {@code JdbcMcpTaskRepository} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value states; it is a state, type, or protocol value of {@code JdbcMcpTaskRepository} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> STATES = Set.of(
            "WORKING",
            "INPUT_REQUIRED",
            "COMPLETED",
            "FAILED",
            "CANCELLED"
    );

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpTaskRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpTaskRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpTaskRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpTaskRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpTaskRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpTaskRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpTaskRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpTaskRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpTaskRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     */
    public void create(McpTaskPO task) {
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
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    public Optional<McpTaskPO> find(String id) {
        List<McpTaskPO> values = jdbc.query("""
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
                """, (result, row) -> new McpTaskPO(
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
     * 中文说明：执行 list 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    public List<McpTaskPO> list(String tenantId, String clientId) {
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
     * 中文说明：执行 claim 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the claim operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.claim(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 transition 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.transition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 cancel 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 map 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private McpTaskPO map(java.sql.ResultSet result)
            throws java.sql.SQLException {
        return new McpTaskPO(
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
     * 中文说明：执行 read 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> read(String value) {
        return value == null ? null : json.map(value);
    }

    /**
     * 中文说明：执行 write 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.write(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 write 的处理结果；returns the result of the operation.
     */
    private String write(Map<String, Object> value) {
        return value == null ? null : json.write(value);
    }

    /**
     * 中文说明：执行 instant 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instant operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.instant(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 instant 的处理结果；returns the result of the operation.
     */
    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /**
     * 中文说明：执行 state 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the state operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.state(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 state 的处理结果；returns the result of the operation.
     */
    public static String state(String value) {
        String candidate = McpJdbcJson.required(value, "state");
        if (!STATES.contains(candidate)) {
            throw new IllegalArgumentException("unsupported task state");
        }
        return candidate;
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code JdbcMcpTaskRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code JdbcMcpTaskRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpTaskRepository.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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


}
