package top.egon.cola.component.gateway.engine.discovery;

/**
 * 中文说明：{@code ProviderCandidateStage} 是枚举类型，位于当前 Gateway 模块的相关包中，负责提供方CandidateStage相关的职责与边界。
 * English summary: {@code ProviderCandidateStage} is an enumeration in the current Gateway module; it owns the provider candidate stage-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum ProviderCandidateStage {

    /**
     * 中文说明：表示 EXACT服务 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value exact service; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    EXACT_SERVICE,

    /**
     * 中文说明：表示 VALID租约 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value valid lease; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    VALID_LEASE,

    /**
     * 中文说明：表示 PROTOCOLMATCH 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value protocol match; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    PROTOCOL_MATCH,

    /**
     * 中文说明：表示 管理端ENABLED 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value admin enabled; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    ADMIN_ENABLED,

    /**
     * 中文说明：表示 LOCATIONANDTAGS 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value location and tags; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    LOCATION_AND_TAGS,

    /**
     * 中文说明：表示 HEALTHY 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value healthy; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    HEALTHY,

    /**
     * 中文说明：表示 准入AVAILABLE 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value admission available; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    ADMISSION_AVAILABLE,

    /**
     * 中文说明：表示 POSITIVEWEIGHT 这一固定值；它属于 {@code ProviderCandidateStage} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value positive weight; it is a state, type, or protocol value of {@code ProviderCandidateStage} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateStage} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateStage}; do not couple callers to its representation when the owning type exposes an API.
     */
    POSITIVE_WEIGHT
}
