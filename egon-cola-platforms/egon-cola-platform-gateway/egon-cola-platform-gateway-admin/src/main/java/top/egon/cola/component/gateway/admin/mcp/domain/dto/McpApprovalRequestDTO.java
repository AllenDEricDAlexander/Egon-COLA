package top.egon.cola.component.gateway.admin.mcp.domain.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 中文说明：{@code McpApprovalRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批请求相关的职责与边界。
 * English summary: {@code McpApprovalRequestDTO} is an immutable data carrier in the current Gateway module; it owns the approval request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param serverCode 参数 服务器Code；parameter server code。
 * @param toolName 参数 工具Name；parameter tool name。
 * @param arguments 参数 arguments；parameter arguments。
 * @param ttlSeconds 参数 ttlSeconds；parameter ttl seconds。
 */
public record McpApprovalRequestDTO(
        /**
         * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String serverCode,
        /**
         * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String toolName,
        /**
         * 中文说明：保存 arguments 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by arguments; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Map<String, Object> arguments,
        /**
         * 中文说明：保存 ttlSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ttl seconds; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @Min(1) @Max(300) long ttlSeconds
) {
}
