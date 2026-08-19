package top.egon.cola.component.gateway.engine.common.traffic.domain;

/**
 * 中文说明：{@code TrafficPolicyType} 是枚举类型，位于当前 Gateway 模块的相关包中，负责流量策略Type相关的职责与边界。
 * English summary: {@code TrafficPolicyType} is an enumeration in the current Gateway module; it owns the traffic policy type-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum TrafficPolicyType {

    /**
     * 中文说明：表示 RATELIMIT 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value rate limit; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    RATE_LIMIT,
    /**
     * 中文说明：表示 超时 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value timeout; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    TIMEOUT,
    /**
     * 中文说明：表示 BULKHEAD 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value bulkhead; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    BULKHEAD,
    /**
     * 中文说明：表示 CIRCUITBREAKER 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value circuit breaker; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    CIRCUIT_BREAKER,
    /**
     * 中文说明：表示 重试 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value retry; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    RETRY,
    /**
     * 中文说明：表示 请求SIZE 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value request size; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    REQUEST_SIZE,
    /**
     * 中文说明：表示 响应SIZE 这一固定值；它属于 {@code TrafficPolicyType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value response size; it is a state, type, or protocol value of {@code TrafficPolicyType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code TrafficPolicyType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrafficPolicyType}; do not couple callers to its representation when the owning type exposes an API.
     */
    RESPONSE_SIZE
}
