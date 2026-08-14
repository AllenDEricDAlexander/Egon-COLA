package top.egon.cola.component.gateway.admin.mcp.domain.dto;


import java.util.Map;

/**
 * 中文说明：{@code McpCapabilityMutationDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责CapabilityMutation相关的职责与边界。
 * English summary: {@code McpCapabilityMutationDTO} is an immutable data carrier in the current Gateway module; it owns the capability mutation-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param serverId 参数 服务器Id；parameter server id。
 * @param name 参数 name；parameter name。
 * @param content 参数 content；parameter content。
 * @param enabled 参数 enabled；parameter enabled。
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record McpCapabilityMutationDTO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String name,
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> content,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedRevision,
        /**
         * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedDraftRevision,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeReason
) {
}
