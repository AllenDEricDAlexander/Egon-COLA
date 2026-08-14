package top.egon.cola.component.gateway.admin.observability.controller.message;


/**
 * 中文说明：{@code GatewayKafkaRecordKey} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Record键相关的职责与边界。
 * English summary: {@code GatewayKafkaRecordKey} is an immutable data carrier in the current Gateway module; it owns the record key-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param topic 参数 topic；parameter topic。
 * @param partition 参数 partition；parameter partition。
 * @param offset 参数 offset；parameter offset。
 */
public record GatewayKafkaRecordKey(
/**
 * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey}; do not couple callers to its representation when the owning type exposes an API.
 */
String topic,
/**
 * 中文说明：保存 partition 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by partition; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey}; do not couple callers to its representation when the owning type exposes an API.
 */
int partition,
/**
 * 中文说明：保存 offset 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by offset; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey}; do not couple callers to its representation when the owning type exposes an API.
 */
long offset) {
}
