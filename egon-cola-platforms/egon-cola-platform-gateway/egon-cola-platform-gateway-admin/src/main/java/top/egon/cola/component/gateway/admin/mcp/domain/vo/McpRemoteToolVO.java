package top.egon.cola.component.gateway.admin.mcp.domain.vo;


import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code McpRemoteToolVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程工具View相关的职责与边界。
 * English summary: {@code McpRemoteToolVO} is an immutable data carrier in the current Gateway module; it owns the remote tool view-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param serverId 参数 服务器Id；parameter server id。
 * @param serverCode 参数 服务器Code；parameter server code。
 * @param name 参数 name；parameter name。
 * @param description 参数 description；parameter description。
 * @param remoteMountId 参数 远程MountId；parameter remote mount id。
 * @param inputSchema 参数 input模式；parameter input schema。
 * @param outputSchema 参数 output模式；parameter output schema。
 * @param annotations 参数 annotations；parameter annotations。
 * @param requiredPermissions 参数 requiredPermissions；parameter required permissions。
 * @param riskLevel 参数 riskLevel；parameter risk level。
 * @param idempotent 参数 idempotent；parameter idempotent。
 * @param enabled 参数 enabled；parameter enabled。
 * @param revision 参数 revision；parameter revision。
 */
public record McpRemoteToolVO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverCode,
        /**
         * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String name,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description,
        /**
         * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String remoteMountId,
        /**
         * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object inputSchema,
        /**
         * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object outputSchema,
        /**
         * 中文说明：保存 annotations 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by annotations; its type is {@code Map<String, String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> annotations,
        /**
         * 中文说明：保存 requiredPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by required permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> requiredPermissions,
        /**
         * 中文说明：保存 riskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String riskLevel,
        /**
         * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean idempotent,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision
) {
}
