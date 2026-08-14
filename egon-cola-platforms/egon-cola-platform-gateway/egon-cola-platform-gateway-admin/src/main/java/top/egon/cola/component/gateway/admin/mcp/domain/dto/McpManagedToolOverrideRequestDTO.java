package top.egon.cola.component.gateway.admin.mcp.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;


/**
 * 中文说明：{@code McpManagedToolOverrideRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具Override请求相关的职责与边界。
 * English summary: {@code McpManagedToolOverrideRequestDTO} is an immutable data carrier in the current Gateway module; it owns the managed tool override request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param enabled 参数 enabled；parameter enabled。
 * @param serverId 参数 服务器Id；parameter server id。
 * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
 * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record McpManagedToolOverrideRequestDTO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String gatewayGroupId,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Boolean enabled,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> additionalPermissions,
        /**
         * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String minimumRiskLevel,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @PositiveOrZero long expectedRevision,
        /**
         * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @PositiveOrZero long expectedDraftRevision,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String changeReason
) {

    /**
     * 中文说明：执行 mutation 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mutation operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 mutation 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideMutationDTO mutation() {
        return new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideMutationDTO(
                gatewayGroupId,
                enabled,
                serverId,
                additionalPermissions,
                minimumRiskLevel,
                expectedRevision,
                expectedDraftRevision,
                changeReason
        );
    }
}
