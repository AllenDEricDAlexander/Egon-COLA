package top.egon.cola.component.gateway.admin.mcp.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;
import java.util.Set;


/**
 * 中文说明：{@code McpRemoteToolRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程工具请求相关的职责与边界。
 * English summary: {@code McpRemoteToolRequestDTO} is an immutable data carrier in the current Gateway module; it owns the remote tool request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param serverId 参数 服务器Id；parameter server id。
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
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record McpRemoteToolRequestDTO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String gatewayGroupId,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String serverId,
        /**
         * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String name,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description,
        /**
         * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String remoteMountId,
        /**
         * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object inputSchema,
        /**
         * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object outputSchema,
        /**
         * 中文说明：保存 annotations 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by annotations; its type is {@code Map<String, String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> annotations,
        /**
         * 中文说明：保存 requiredPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by required permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> requiredPermissions,
        /**
         * 中文说明：保存 riskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String riskLevel,
        /**
         * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean idempotent,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Boolean enabled,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @PositiveOrZero long expectedRevision,
        /**
         * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @PositiveOrZero long expectedDraftRevision,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String changeReason
) {

    /**
     * 中文说明：执行 mutation 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mutation operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO.mutation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 mutation 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolMutationDTO mutation() {
        return new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolMutationDTO(
                gatewayGroupId,
                serverId,
                name,
                description,
                remoteMountId,
                inputSchema,
                outputSchema,
                annotations,
                requiredPermissions,
                riskLevel,
                idempotent,
                enabled,
                expectedRevision,
                expectedDraftRevision,
                changeReason
        );
    }
}
