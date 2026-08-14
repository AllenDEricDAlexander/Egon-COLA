package top.egon.cola.component.gateway.admin.release.domain.po;


/**
 * 中文说明：{@code GatewayRecoverableReleaseAttemptPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayRecoverableReleaseAttemptPO相关的职责与边界。
 * English summary: {@code GatewayRecoverableReleaseAttemptPO} is an immutable data carrier in the current Gateway module; it owns the recoverable attempt-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param releaseId 参数 发布Id；parameter release id。
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param attemptNo 参数 attemptNo；parameter attempt no。
 */
public record GatewayRecoverableReleaseAttemptPO(
        /**
         * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String releaseId,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int attemptNo
) {
}
