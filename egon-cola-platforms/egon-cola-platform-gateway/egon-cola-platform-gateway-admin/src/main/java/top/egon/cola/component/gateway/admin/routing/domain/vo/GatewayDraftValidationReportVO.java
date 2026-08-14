package top.egon.cola.component.gateway.admin.routing.domain.vo;


import java.util.List;

/**
 * 中文说明：{@code GatewayDraftValidationReportVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Validation报告相关的职责与边界。
 * English summary: {@code GatewayDraftValidationReportVO} is an immutable data carrier in the current Gateway module; it owns the validation report-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param valid 参数 valid；parameter valid。
 * @param revision 参数 revision；parameter revision。
 * @param errors 参数 errors；parameter errors。
 * @param warnings 参数 warnings；parameter warnings。
 * @param draftSha256 参数 草稿Sha256；parameter draft sha256。
 */
public record GatewayDraftValidationReportVO(
        /**
         * 中文说明：保存 valid 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by valid; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean valid,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision,
        /**
         * 中文说明：保存 errors 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayDraftValidationIssueVO>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by errors; its type is {@code List<GatewayDraftValidationIssueVO>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayDraftValidationIssueVO> errors,
        /**
         * 中文说明：保存 warnings 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayDraftValidationIssueVO>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by warnings; its type is {@code List<GatewayDraftValidationIssueVO>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayDraftValidationIssueVO> warnings,
        /**
         * 中文说明：保存 草稿Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by draft sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String draftSha256
) {
}
