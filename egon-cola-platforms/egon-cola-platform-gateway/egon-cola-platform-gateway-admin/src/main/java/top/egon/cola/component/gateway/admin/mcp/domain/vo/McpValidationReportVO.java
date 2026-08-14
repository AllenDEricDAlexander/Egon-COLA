package top.egon.cola.component.gateway.admin.mcp.domain.vo;


import java.util.List;


/**
 * 中文说明：{@code McpValidationReportVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Validation报告相关的职责与边界。
 * English summary: {@code McpValidationReportVO} is an immutable data carrier in the current Gateway module; it owns the validation report-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param valid 参数 valid；parameter valid。
 * @param findings 参数 findings；parameter findings。
 */
public record McpValidationReportVO(
        /**
         * 中文说明：保存 valid 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by valid; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean valid,
        /**
         * 中文说明：保存 findings 对应的状态、依赖或配置值；字段类型为 {@code List<McpValidationFindingVO>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by findings; its type is {@code List<McpValidationFindingVO>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<McpValidationFindingVO> findings
) {
}
