package top.egon.cola.component.gateway.admin.mcp.domain.vo;


import java.util.Map;

/**
 * 中文说明：{@code McpProtocolInspectionVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Inspection相关的职责与边界。
 * English summary: {@code McpProtocolInspectionVO} is an immutable data carrier in the current Gateway module; it owns the inspection-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param path 参数 path；parameter path。
 * @param headers 参数 headers；parameter headers。
 * @param body 参数 body；parameter body。
 * @param releaseCandidate 参数 发布Candidate；parameter release candidate。
 */
public record McpProtocolInspectionVO(
        /**
         * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String path,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> headers,
        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> body,
        /**
         * 中文说明：保存 发布Candidate 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release candidate; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpProtocolInspectionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean releaseCandidate
) {
}
