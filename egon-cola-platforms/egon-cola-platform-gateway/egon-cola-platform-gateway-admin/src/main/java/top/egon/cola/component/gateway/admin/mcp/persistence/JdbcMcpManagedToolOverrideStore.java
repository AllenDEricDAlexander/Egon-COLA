package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code JdbcMcpManagedToolOverrideStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCPManaged工具Override存储相关的职责与边界。
 * English summary: {@code JdbcMcpManagedToolOverrideStore} is a jdbc mcp managed tool override store store in the current Gateway module; it owns the jdbc mcp managed tool override store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpManagedToolOverrideStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpManagedToolOverrideStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpManagedToolOverrideStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpManagedToolOverrideStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpManagedToolOverrideStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpManagedToolOverrideStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpManagedToolOverrideStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpManagedToolOverrideStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
    public List<ManagedToolOverride> load(String gatewayGroupId) {
        return jdbc.query("""
                SELECT tool_id, gateway_group_id, operation_id, server_id,
                       additional_permissions::text AS additional_permissions,
                       minimum_risk_level, enabled, revision
                  FROM gateway_mcp_managed_tool_override
                 WHERE gateway_group_id = ?
                 ORDER BY operation_id
                """, (result, row) -> new ManagedToolOverride(
                result.getString("tool_id"),
                result.getString("gateway_group_id"),
                result.getString("operation_id"),
                result.getString("server_id"),
                json.stringSet(
                        result.getString("additional_permissions")
                ),
                result.getString("minimum_risk_level"),
                result.getObject("enabled", Boolean.class),
                result.getLong("revision")
        ), McpJdbcJson.required(gatewayGroupId, "gatewayGroupId"));
    }

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param override 参数 override；parameter override。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save 的处理结果；returns the result of the operation.
     */
    public DraftMutation save(
            ManagedToolOverride override,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(override, "override");
        validateExpectedRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_managed_tool_override
                   SET tool_id = ?,
                       server_id = ?,
                       additional_permissions = ?::jsonb,
                       minimum_risk_level = ?,
                       enabled = ?,
                       revision = revision + 1,
                       updated_at = ?,
                       updated_by = ?
                 WHERE gateway_group_id = ?
                   AND operation_id = ?
                   AND revision = ?
                """,
                override.toolId(),
                override.serverId(),
                json.write(override.additionalPermissions()),
                override.minimumRiskLevel(),
                override.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                override.gatewayGroupId(),
                override.operationId(),
                expectedRevision
        );
        if (updated == 1) {
            return new DraftMutation(override.toolId(), expectedRevision + 1);
        }
        Long current = currentRevision(
                override.gatewayGroupId(),
                override.operationId()
        );
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_managed_tool_override(
                    tool_id, gateway_group_id, operation_id, server_id,
                    additional_permissions, minimum_risk_level, enabled,
                    revision, created_at, created_by, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, 0, ?, ?, ?, ?)
                """,
                override.toolId(),
                override.gatewayGroupId(),
                override.operationId(),
                override.serverId(),
                json.write(override.additionalPermissions()),
                override.minimumRiskLevel(),
                override.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new DraftMutation(override.toolId(), 0);
    }

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 delete 的处理结果；returns the result of the operation.
     */
    public DraftMutation delete(
            String toolId,
            String gatewayGroupId,
            String operationId,
            long expectedRevision) {
        validateExpectedRevision(expectedRevision);
        int deleted = jdbc.update("""
                DELETE FROM gateway_mcp_managed_tool_override
                 WHERE gateway_group_id = ?
                   AND operation_id = ?
                   AND revision = ?
                """,
                McpJdbcJson.required(gatewayGroupId, "gatewayGroupId"),
                McpJdbcJson.required(operationId, "operationId"),
                expectedRevision
        );
        if (deleted != 1) {
            throw revisionConflict(currentRevision(
                    gatewayGroupId,
                    operationId
            ));
        }
        return new DraftMutation(toolId, expectedRevision + 1);
    }

    /**
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 currentRevision 的处理结果；returns the result of the operation.
     */
    private Long currentRevision(
            String gatewayGroupId,
            String operationId) {
        return jdbc.query(
                "SELECT revision FROM gateway_mcp_managed_tool_override "
                        + "WHERE gateway_group_id = ? AND operation_id = ?",
                (result, row) -> result.getLong("revision"),
                gatewayGroupId,
                operationId
        ).stream().findFirst().orElse(null);
    }

    /**
     * 中文说明：执行 validateExpectedRevision 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate expected revision operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.validateExpectedRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    private void validateExpectedRevision(long expectedRevision) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    /**
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param currentRevision 参数 currentRevision；parameter current revision。
     * @return 返回 revisionConflict 的处理结果；returns the result of the operation.
     */
    private GatewayAdminRevisionConflictException revisionConflict(
            Long currentRevision) {
        return new GatewayAdminRevisionConflictException(
                currentRevision == null ? -1 : currentRevision
        );
    }

    /**
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpManagedToolOverrideStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideStore.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    /**
     * 中文说明：{@code ManagedToolOverride} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具Override相关的职责与边界。
     * English summary: {@code ManagedToolOverride} is an immutable data carrier in the current Gateway module; it owns the managed tool override-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
     * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public record ManagedToolOverride(
            /**
             * 中文说明：保存 工具Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool id; its type is {@code String}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            String toolId,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> additionalPermissions,
            /**
             * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            String minimumRiskLevel,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            Boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpManagedToolOverrideStore.ManagedToolOverride} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param toolId 参数 工具Id；parameter tool id。
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param operationId 参数 操作Id；parameter operation id。
         * @param serverId 参数 服务器Id；parameter server id。
         * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
         * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
         * @param enabled 参数 enabled；parameter enabled。
         * @param revision 参数 revision；parameter revision。
         */
        public ManagedToolOverride {
            toolId = McpJdbcJson.required(toolId, "toolId");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            operationId = McpJdbcJson.required(operationId, "operationId");
            additionalPermissions = Set.copyOf(Objects.requireNonNull(
                    additionalPermissions,
                    "additionalPermissions"
            ));
            if (Boolean.TRUE.equals(enabled)) {
                throw new IllegalArgumentException(
                        "managed Tool override cannot enable a Tool"
                );
            }
            if (serverId == null && additionalPermissions.isEmpty()
                    && minimumRiskLevel == null && enabled == null) {
                throw new IllegalArgumentException(
                        "managed Tool override must tighten at least one field"
                );
            }
            if (revision < 0) {
                throw new IllegalArgumentException(
                        "revision must not be negative"
                );
            }
        }
    }

    /**
     * 中文说明：{@code DraftMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿Mutation相关的职责与边界。
     * English summary: {@code DraftMutation} is an immutable data carrier in the current Gateway module; it owns the draft mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param revision 参数 revision；parameter revision。
     */
    public record DraftMutation(
    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpManagedToolOverrideStore.DraftMutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpManagedToolOverrideStore.DraftMutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.DraftMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.DraftMutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    String id,
    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpManagedToolOverrideStore.DraftMutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpManagedToolOverrideStore.DraftMutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideStore.DraftMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideStore.DraftMutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    long revision) {
    }
}
