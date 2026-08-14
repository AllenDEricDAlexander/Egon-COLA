package top.egon.cola.component.gateway.admin.mcp.domain.po;


import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.McpJdbcJson;

import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code McpManagedToolOverridePO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具Override相关的职责与边界。
 * English summary: {@code McpManagedToolOverridePO} is an immutable data carrier in the current Gateway module; it owns the managed tool override-related responsibility and boundary.
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
public record McpManagedToolOverridePO(
        /**
         * 中文说明：保存 工具Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tool id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String toolId,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationId,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> additionalPermissions,
        /**
         * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String minimumRiskLevel,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Boolean enabled,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
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
    public McpManagedToolOverridePO {
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
