package top.egon.cola.component.gateway.engine.traffic;

/**
 * 中文说明：{@code TrafficPolicyScope} 是枚举类型，位于当前 Gateway 模块的相关包中，负责流量策略Scope相关的职责与边界。
 * English summary: {@code TrafficPolicyScope} is an enumeration in the current Gateway module; it owns the traffic policy scope-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum TrafficPolicyScope {

    /**
     * 中文说明：表示 GLOBAL 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value global; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    GLOBAL,
    /**
     * 中文说明：表示 网关GROUP 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value gateway group; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    GATEWAY_GROUP,
    /**
     * 中文说明：表示 APPLICATION 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value application; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    APPLICATION,
    /**
     * 中文说明：表示 BUSINESSDOMAIN 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value business domain; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    BUSINESS_DOMAIN,
    /**
     * 中文说明：表示 ENTITYDOMAIN 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value entity domain; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    ENTITY_DOMAIN,
    /**
     * 中文说明：表示 接口GROUP 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value interface group; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    INTERFACE_GROUP,
    /**
     * 中文说明：表示 操作 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value operation; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    OPERATION,
    /**
     * 中文说明：表示 路由 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value route; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    ROUTE,
    /**
     * 中文说明：表示 CALLER 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value caller; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    CALLER,
    /**
     * 中文说明：表示 提供方服务 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value provider service; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    PROVIDER_SERVICE,
    /**
     * 中文说明：表示 提供方INSTANCE 这一固定值；它属于 {@code TrafficPolicyScope} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value provider instance; it is a state, type, or protocol value of {@code TrafficPolicyScope} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyScope}; do not couple callers to its representation when the owning type exposes an API.
     */
    PROVIDER_INSTANCE
}
