package top.egon.cola.component.gateway.admin.routing.domain.vo;


/**
 * 中文说明：{@code GatewayDraftValidationIssueVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ValidationIssue相关的职责与边界。
 * English summary: {@code GatewayDraftValidationIssueVO} is an immutable data carrier in the current Gateway module; it owns the validation issue-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param path 参数 path；parameter path。
 * @param code 参数 code；parameter code。
 * @param message 参数 消息；parameter message。
 */
public record GatewayDraftValidationIssueVO(
        /**
         * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String path,
        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String code,
        /**
         * 中文说明：保存 消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by message; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String message
) {
}
