package top.egon.cola.component.gateway.admin.mcp.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code JdbcMcpApprovalStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP审批存储相关的职责与边界。
 * English summary: {@code JdbcMcpApprovalStore} is a jdbc mcp approval store store in the current Gateway module; it owns the jdbc mcp approval store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpApprovalStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpApprovalStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpApprovalStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：创建 {@code JdbcMcpApprovalStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpApprovalStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcMcpApprovalStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    /**
     * 中文说明：执行 issue 操作；该方法是 {@code JdbcMcpApprovalStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the issue operation; this method is the invocation entry point on {@code JdbcMcpApprovalStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalStore.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param approval 参数 审批；parameter approval。
     */
    public void issue(Approval approval) {
        Objects.requireNonNull(approval, "approval");
        jdbc.update("""
                INSERT INTO gateway_mcp_approval(
                    id, token_digest, subject_id, tenant_id, client_id,
                    server_code, tool_name, argument_digest, status,
                    revision, issued_at, expires_at, consumed_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, ?, NULL
                )
                """,
                approval.id(),
                approval.tokenDigest(),
                approval.subjectId(),
                approval.tenantId(),
                approval.clientId(),
                approval.serverCode(),
                approval.toolName(),
                approval.argumentDigest(),
                McpJdbcJson.timestamp(approval.issuedAt()),
                McpJdbcJson.timestamp(approval.expiresAt())
        );
    }

    /**
     * 中文说明：执行 consume 操作；该方法是 {@code JdbcMcpApprovalStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consume operation; this method is the invocation entry point on {@code JdbcMcpApprovalStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalStore.consume(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tokenDigest 参数 tokenDigest；parameter token digest。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param argumentDigest 参数 argumentDigest；parameter argument digest。
     * @param now 参数 now；parameter now。
     * @return 返回 consume 的处理结果；returns the result of the operation.
     */
    public boolean consume(
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'CONSUMED', consumed_at = ?,
                       revision = revision + 1
                 WHERE token_digest = ?
                   AND subject_id = ?
                   AND tenant_id = ?
                   AND client_id = ?
                   AND server_code = ?
                   AND tool_name = ?
                   AND argument_digest = ?
                   AND status = 'PENDING'
                   AND expires_at > ?
                """,
                McpJdbcJson.timestamp(now),
                digest(tokenDigest, "tokenDigest"),
                McpJdbcJson.required(subjectId, "subjectId"),
                McpJdbcJson.required(tenantId, "tenantId"),
                McpJdbcJson.required(clientId, "clientId"),
                McpJdbcJson.required(serverCode, "serverCode"),
                McpJdbcJson.required(toolName, "toolName"),
                digest(argumentDigest, "argumentDigest"),
                McpJdbcJson.timestamp(now)
        ) == 1;
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpApprovalStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpApprovalStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    public Optional<Approval> find(String id) {
        List<Approval> values = jdbc.query("""
                SELECT id, token_digest, subject_id, tenant_id, client_id,
                       server_code, tool_name, argument_digest,
                       issued_at, expires_at
                  FROM gateway_mcp_approval
                 WHERE id = ?
                """, (result, row) -> new Approval(
                result.getString("id"),
                result.getString("token_digest"),
                result.getString("subject_id"),
                result.getString("tenant_id"),
                result.getString("client_id"),
                result.getString("server_code"),
                result.getString("tool_name"),
                result.getString("argument_digest"),
                result.getTimestamp("issued_at").toInstant(),
                result.getTimestamp("expires_at").toInstant()
        ), id);
        return values.stream().findFirst();
    }

    /**
     * 中文说明：执行 expire 操作；该方法是 {@code JdbcMcpApprovalStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the expire operation; this method is the invocation entry point on {@code JdbcMcpApprovalStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalStore.expire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 expire 的处理结果；returns the result of the operation.
     */
    public int expire(Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'EXPIRED', revision = revision + 1
                 WHERE status = 'PENDING' AND expires_at <= ?
                """, McpJdbcJson.timestamp(now));
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code JdbcMcpApprovalStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code JdbcMcpApprovalStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalStore.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 revoke 的处理结果；returns the result of the operation.
     */
    public boolean revoke(String id, long expectedRevision) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'REVOKED', revision = revision + 1
                 WHERE id = ? AND revision = ? AND status = 'PENDING'
                """, id, expectedRevision) == 1;
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code JdbcMcpApprovalStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code JdbcMcpApprovalStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalStore.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：{@code Approval} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批相关的职责与边界。
     * English summary: {@code Approval} is an immutable data carrier in the current Gateway module; it owns the approval-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param tokenDigest 参数 tokenDigest；parameter token digest。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param argumentDigest 参数 argumentDigest；parameter argument digest。
     * @param issuedAt 参数 issuedAt；parameter issued at。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     */
    public record Approval(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 tokenDigest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by token digest; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tokenDigest,
            /**
             * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectId,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String toolName,
            /**
             * 中文说明：保存 argumentDigest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by argument digest; its type is {@code String}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            String argumentDigest,
            /**
             * 中文说明：保存 issuedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by issued at; its type is {@code Instant}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant issuedAt,
            /**
             * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpApprovalStore.Approval} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code JdbcMcpApprovalStore.Approval} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalStore.Approval} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalStore.Approval}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expiresAt
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpApprovalStore.Approval} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpApprovalStore.Approval} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param tokenDigest 参数 tokenDigest；parameter token digest。
         * @param subjectId 参数 subjectId；parameter subject id。
         * @param tenantId 参数 tenantId；parameter tenant id。
         * @param clientId 参数 客户端Id；parameter client id。
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param toolName 参数 工具Name；parameter tool name。
         * @param argumentDigest 参数 argumentDigest；parameter argument digest。
         * @param issuedAt 参数 issuedAt；parameter issued at。
         * @param expiresAt 参数 expiresAt；parameter expires at。
         */
        public Approval {
            id = McpJdbcJson.required(id, "id");
            tokenDigest = digest(tokenDigest, "tokenDigest");
            subjectId = McpJdbcJson.required(subjectId, "subjectId");
            tenantId = McpJdbcJson.required(tenantId, "tenantId");
            clientId = McpJdbcJson.required(clientId, "clientId");
            serverCode = McpJdbcJson.required(serverCode, "serverCode");
            toolName = McpJdbcJson.required(toolName, "toolName");
            argumentDigest = digest(argumentDigest, "argumentDigest");
            issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
            if (!expiresAt.isAfter(issuedAt)) {
                throw new IllegalArgumentException(
                        "expiresAt must be after issuedAt"
                );
            }
        }
    }
}
