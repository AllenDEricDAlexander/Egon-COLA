package top.egon.cola.component.gateway.admin.mcp.domain.vo;


import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.util.Set;


/**
 * 中文说明：{@code McpManagedToolProjectionVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具投影相关的职责与边界。
 * English summary: {@code McpManagedToolProjectionVO} is an immutable data carrier in the current Gateway module; it owns the managed tool projection-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param operationKey 参数 操作键；parameter operation key。
 * @param codeServerId 参数 code服务器Id；parameter code server id。
 * @param codeServerCode 参数 code服务器Code；parameter code server code。
 * @param serverId 参数 服务器Id；parameter server id。
 * @param codePermissions 参数 codePermissions；parameter code permissions。
 * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
 * @param codeRiskLevel 参数 codeRiskLevel；parameter code risk level。
 * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
 * @param overrideRevision 参数 overrideRevision；parameter override revision。
 * @param tool 参数 工具；parameter tool。
 */
public record McpManagedToolProjectionVO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationKey,
        /**
         * 中文说明：保存 code服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String codeServerId,
        /**
         * 中文说明：保存 code服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code server code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String codeServerCode,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 codePermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> codePermissions,
        /**
         * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> additionalPermissions,
        /**
         * 中文说明：保存 codeRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String codeRiskLevel,
        /**
         * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String minimumRiskLevel,
        /**
         * 中文说明：保存 overrideRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by override revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long overrideRevision,
        /**
         * 中文说明：保存 工具 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeTool}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tool; its type is {@code McpRuntimeTool}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        McpRuntimeTool tool
) {
}
