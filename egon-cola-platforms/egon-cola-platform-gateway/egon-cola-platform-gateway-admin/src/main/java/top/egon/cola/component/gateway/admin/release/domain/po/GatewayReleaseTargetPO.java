package top.egon.cola.component.gateway.admin.release.domain.po;


import java.time.Instant;


/**
 * 中文说明：{@code GatewayReleaseTargetPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayReleaseTargetPO相关的职责与边界。
 * English summary: {@code GatewayReleaseTargetPO} is an immutable data carrier in the current Gateway module; it owns the target record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param instanceId 参数 instanceId；parameter instance id。
 * @param leaseId 参数 租约Id；parameter lease id。
 * @param status 参数 status；parameter status。
 * @param appliedVersion 参数 appliedVersion；parameter applied version。
 * @param appliedArtifactSha256 参数 applied制品Sha256；parameter applied artifact sha256。
 * @param errorCode 参数 errorCode；parameter error code。
 * @param observedAt 参数 observedAt；parameter observed at。
 */
public record GatewayReleaseTargetPO(
        /**
         * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String instanceId,
        /**
         * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String leaseId,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 appliedVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by applied version; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long appliedVersion,
        /**
         * 中文说明：保存 applied制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by applied artifact sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appliedArtifactSha256,
        /**
         * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorCode,
        /**
         * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant observedAt
) {
}
