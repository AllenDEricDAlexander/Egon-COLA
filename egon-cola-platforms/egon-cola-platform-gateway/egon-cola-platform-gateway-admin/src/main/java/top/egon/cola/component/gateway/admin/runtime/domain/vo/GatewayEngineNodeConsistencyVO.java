package top.egon.cola.component.gateway.admin.runtime.domain.vo;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayEngineNodeConsistencyVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责引擎NodeConsistency相关的职责与边界。
 * English summary: {@code GatewayEngineNodeConsistencyVO} is an immutable data carrier in the current Gateway module; it owns the engine node consistency-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param instanceId 参数 instanceId；parameter instance id。
 * @param leaseId 参数 租约Id；parameter lease id。
 * @param leaseStatus 参数 租约Status；parameter lease status。
 * @param status 参数 status；parameter status。
 * @param reason 参数 reason；parameter reason。
 * @param activeReleaseId 参数 active发布Id；parameter active release id。
 * @param activeRuleVersion 参数 active规则Version；parameter active rule version。
 * @param activeRuleChecksum 参数 active规则Checksum；parameter active rule checksum。
 * @param lastApplyStatus 参数 lastApplyStatus；parameter last apply status。
 * @param lastAckAt 参数 lastAckAt；parameter last ack at。
 */
public record GatewayEngineNodeConsistencyVO(
        /**
         * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String instanceId,
        /**
         * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String leaseId,
        /**
         * 中文说明：保存 租约Status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String leaseStatus,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 reason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String reason,
        /**
         * 中文说明：保存 active发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String activeReleaseId,
        /**
         * 中文说明：保存 active规则Version 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active rule version; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long activeRuleVersion,
        /**
         * 中文说明：保存 active规则Checksum 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active rule checksum; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String activeRuleChecksum,
        /**
         * 中文说明：保存 lastApplyStatus 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last apply status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String lastApplyStatus,
        /**
         * 中文说明：保存 lastAckAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last ack at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant lastAckAt
) {
}
