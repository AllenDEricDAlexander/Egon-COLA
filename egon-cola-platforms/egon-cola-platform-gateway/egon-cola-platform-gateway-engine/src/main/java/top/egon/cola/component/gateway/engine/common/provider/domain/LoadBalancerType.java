package top.egon.cola.component.gateway.engine.common.provider.domain;

/**
 * 中文说明：{@code LoadBalancerType} 是枚举类型，位于当前 Gateway 模块的相关包中，负责LoadBalancerType相关的职责与边界。
 * English summary: {@code LoadBalancerType} is an enumeration in the current Gateway module; it owns the load balancer type-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum LoadBalancerType {

    /**
     * 中文说明：表示 ROUNDROBIN 这一固定值；它属于 {@code LoadBalancerType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value round robin; it is a state, type, or protocol value of {@code LoadBalancerType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code LoadBalancerType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LoadBalancerType}; do not couple callers to its representation when the owning type exposes an API.
     */
    ROUND_ROBIN,

    /**
     * 中文说明：表示 SMOOTHWEIGHTEDROUNDROBIN 这一固定值；它属于 {@code LoadBalancerType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value smooth weighted round robin; it is a state, type, or protocol value of {@code LoadBalancerType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code LoadBalancerType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LoadBalancerType}; do not couple callers to its representation when the owning type exposes an API.
     */
    SMOOTH_WEIGHTED_ROUND_ROBIN,

    /**
     * 中文说明：表示 RANDOM 这一固定值；它属于 {@code LoadBalancerType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value random; it is a state, type, or protocol value of {@code LoadBalancerType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code LoadBalancerType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LoadBalancerType}; do not couple callers to its representation when the owning type exposes an API.
     */
    RANDOM,

    /**
     * 中文说明：表示 LEASTINFLIGHT 这一固定值；它属于 {@code LoadBalancerType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value least in flight; it is a state, type, or protocol value of {@code LoadBalancerType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code LoadBalancerType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LoadBalancerType}; do not couple callers to its representation when the owning type exposes an API.
     */
    LEAST_IN_FLIGHT
}
