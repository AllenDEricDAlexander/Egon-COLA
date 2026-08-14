package top.egon.cola.component.gateway.admin.observability.repository.jdbc;


import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;


/**
 * 中文说明：{@code GatewayObservabilitySqlFilter} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Sql过滤器相关的职责与边界。
 * English summary: {@code GatewayObservabilitySqlFilter} is an immutable data carrier in the current Gateway module; it owns the sql filter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param where 参数 where；parameter where。
 * @param parameters 参数 parameters；parameter parameters。
 */
public record GatewayObservabilitySqlFilter(
        /**
         * 中文说明：保存 where 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by where; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter}; do not couple callers to its representation when the owning type exposes an API.
         */
        String where,
        /**
         * 中文说明：保存 parameters 对应的状态、依赖或配置值；字段类型为 {@code MapSqlParameterSource}，由 {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by parameters; its type is {@code MapSqlParameterSource}, and {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.repository.jdbc.GatewayObservabilitySqlFilter}; do not couple callers to its representation when the owning type exposes an API.
         */
        MapSqlParameterSource parameters
) {
}
