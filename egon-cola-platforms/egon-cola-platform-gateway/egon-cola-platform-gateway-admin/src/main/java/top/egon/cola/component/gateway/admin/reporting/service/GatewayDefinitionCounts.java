package top.egon.cola.component.gateway.admin.reporting.service;


/**
 * 中文说明：{@code GatewayDefinitionCounts} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责定义Counts相关的职责与边界。
 * English summary: {@code GatewayDefinitionCounts} is an immutable data carrier in the current Gateway module; it owns the definition counts-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param businesses 参数 businesses；parameter businesses。
 * @param entities 参数 entities；parameter entities。
 * @param groups 参数 groups；parameter groups。
 * @param operations 参数 operations；parameter operations。
 */
public record GatewayDefinitionCounts(
        /**
         * 中文说明：保存 businesses 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by businesses; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        int businesses,
        /**
         * 中文说明：保存 entities 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by entities; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        int entities,
        /**
         * 中文说明：保存 groups 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by groups; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        int groups,
        /**
         * 中文说明：保存 operations 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operations; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
         */
        int operations
) {
}
