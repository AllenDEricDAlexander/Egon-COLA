package top.egon.cola.component.gateway.admin.runtime.service;


/**
 * 中文说明：{@code GatewayRuleExpectation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责规则Expectation相关的职责与边界。
 * English summary: {@code GatewayRuleExpectation} is an immutable data carrier in the current Gateway module; it owns the rule expectation-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param version 参数 version；parameter version。
 * @param artifactSha256 参数 制品Sha256；parameter artifact sha256。
 */
public record GatewayRuleExpectation(
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long version,
        /**
         * 中文说明：保存 制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by artifact sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation}; do not couple callers to its representation when the owning type exposes an API.
         */
        String artifactSha256
) {
}
