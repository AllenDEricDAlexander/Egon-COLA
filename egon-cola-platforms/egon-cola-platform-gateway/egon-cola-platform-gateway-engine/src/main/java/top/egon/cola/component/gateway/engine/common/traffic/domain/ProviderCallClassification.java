package top.egon.cola.component.gateway.engine.common.traffic.domain;

/**
 * 中文说明：{@code ProviderCallClassification} 是枚举类型，位于当前 Gateway 模块的相关包中，负责提供方调用Classification相关的职责与边界。
 * English summary: {@code ProviderCallClassification} is an enumeration in the current Gateway module; it owns the provider call classification-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum ProviderCallClassification {

    /**
     * 中文说明：表示 SUCCESS 这一固定值；它属于 {@code ProviderCallClassification} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value success; it is a state, type, or protocol value of {@code ProviderCallClassification} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallClassification} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallClassification}; do not couple callers to its representation when the owning type exposes an API.
     */
    SUCCESS,

    /**
     * 中文说明：表示 RETRYABLEFAILURE 这一固定值；它属于 {@code ProviderCallClassification} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value retryable failure; it is a state, type, or protocol value of {@code ProviderCallClassification} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallClassification} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallClassification}; do not couple callers to its representation when the owning type exposes an API.
     */
    RETRYABLE_FAILURE,

    /**
     * 中文说明：表示 BUSINESSFAILURE 这一固定值；它属于 {@code ProviderCallClassification} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value business failure; it is a state, type, or protocol value of {@code ProviderCallClassification} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallClassification} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallClassification}; do not couple callers to its representation when the owning type exposes an API.
     */
    BUSINESS_FAILURE,

    /**
     * 中文说明：表示 CANCELLED 这一固定值；它属于 {@code ProviderCallClassification} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value cancelled; it is a state, type, or protocol value of {@code ProviderCallClassification} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallClassification} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallClassification}; do not couple callers to its representation when the owning type exposes an API.
     */
    CANCELLED
}
