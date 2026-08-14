package top.egon.cola.component.gateway.admin.mcp.domain.po;


import java.time.Instant;
import java.util.Map;

/**
 * 中文说明：{@code McpRemoteCapabilityPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程Capability相关的职责与边界。
 * English summary: {@code McpRemoteCapabilityPO} is an immutable data carrier in the current Gateway module; it owns the remote capability-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param providerId 参数 提供方Id；parameter provider id。
 * @param primitiveType 参数 primitiveType；parameter primitive type。
 * @param remoteName 参数 远程Name；parameter remote name。
 * @param descriptor 参数 descriptor；parameter descriptor。
 * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
 * @param syncedAt 参数 syncedAt；parameter synced at。
 */
public record McpRemoteCapabilityPO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String providerId,
        /**
         * 中文说明：保存 primitiveType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by primitive type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String primitiveType,
        /**
         * 中文说明：保存 远程Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String remoteName,
        /**
         * 中文说明：保存 descriptor 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by descriptor; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> descriptor,
        /**
         * 中文说明：保存 capabilityFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by capability fingerprint; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String capabilityFingerprint,
        /**
         * 中文说明：保存 syncedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by synced at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant syncedAt
) {
}
