package top.egon.cola.component.gateway.engine.discovery;

/**
 * 中文说明：{@code ProviderCallOutcome} 是枚举类型，位于当前 Gateway 模块的相关包中，负责提供方调用Outcome相关的职责与边界。
 * English summary: {@code ProviderCallOutcome} is an enumeration in the current Gateway module; it owns the provider call outcome-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum ProviderCallOutcome {

    /**
     * 中文说明：表示 SUCCESS 这一固定值；它属于 {@code ProviderCallOutcome} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value success; it is a state, type, or protocol value of {@code ProviderCallOutcome} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallOutcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallOutcome}; do not couple callers to its representation when the owning type exposes an API.
     */
    SUCCESS,

    /**
     * 中文说明：表示 RETRYABLEFAILURE 这一固定值；它属于 {@code ProviderCallOutcome} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value retryable failure; it is a state, type, or protocol value of {@code ProviderCallOutcome} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallOutcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallOutcome}; do not couple callers to its representation when the owning type exposes an API.
     */
    RETRYABLE_FAILURE,

    /**
     * 中文说明：表示 BUSINESSREJECTION 这一固定值；它属于 {@code ProviderCallOutcome} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value business rejection; it is a state, type, or protocol value of {@code ProviderCallOutcome} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallOutcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallOutcome}; do not couple callers to its representation when the owning type exposes an API.
     */
    BUSINESS_REJECTION,

    /**
     * 中文说明：表示 CANCELLED 这一固定值；它属于 {@code ProviderCallOutcome} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value cancelled; it is a state, type, or protocol value of {@code ProviderCallOutcome} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCallOutcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCallOutcome}; do not couple callers to its representation when the owning type exposes an API.
     */
    CANCELLED
}
