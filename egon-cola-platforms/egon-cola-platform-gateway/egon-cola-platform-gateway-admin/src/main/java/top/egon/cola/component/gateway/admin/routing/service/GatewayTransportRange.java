package top.egon.cola.component.gateway.admin.routing.service;


/**
 * 中文说明：{@code GatewayTransportRange} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Range相关的职责与边界。
 * English summary: {@code GatewayTransportRange} is an immutable data carrier in the current Gateway module; it owns the range-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param minimum 参数 minimum；parameter minimum。
 * @param maximum 参数 maximum；parameter maximum。
 */
public record GatewayTransportRange(
/**
 * 中文说明：保存 minimum 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by minimum; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange}; do not couple callers to its representation when the owning type exposes an API.
 */
long minimum,
/**
 * 中文说明：保存 maximum 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by maximum; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.service.GatewayTransportRange}; do not couple callers to its representation when the owning type exposes an API.
 */
long maximum) {
}
