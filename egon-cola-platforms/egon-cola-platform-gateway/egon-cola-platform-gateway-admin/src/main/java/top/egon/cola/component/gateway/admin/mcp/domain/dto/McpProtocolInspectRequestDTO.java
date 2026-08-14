package top.egon.cola.component.gateway.admin.mcp.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 中文说明：{@code McpProtocolInspectRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Inspect请求相关的职责与边界。
 * English summary: {@code McpProtocolInspectRequestDTO} is an immutable data carrier in the current Gateway module; it owns the inspect request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param dialect 参数 dialect；parameter dialect。
 * @param method 参数 方法；parameter method。
 * @param params 参数 params；parameter params。
 */
public record McpProtocolInspectRequestDTO(
        /**
         * 中文说明：保存 dialect 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by dialect; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String dialect,
        /**
         * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String method,
        /**
         * 中文说明：保存 params 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by params; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpProtocolInspectRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Map<String, Object> params
) {
}
