package top.egon.cola.component.gateway.admin.release.domain.po;


import java.time.Instant;
import java.util.List;


/**
 * 中文说明：{@code GatewayReleaseAttemptPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayReleaseAttemptPO相关的职责与边界。
 * English summary: {@code GatewayReleaseAttemptPO} is an immutable data carrier in the current Gateway module; it owns the attempt record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param attemptNo 参数 attemptNo；parameter attempt no。
 * @param status 参数 status；parameter status。
 * @param changeId 参数 changeId；parameter change id。
 * @param startedAt 参数 startedAt；parameter started at。
 * @param completedAt 参数 completedAt；parameter completed at。
 * @param errorCode 参数 errorCode；parameter error code。
 * @param errorMessage 参数 error消息；parameter error message。
 * @param targets 参数 targets；parameter targets。
 */
public record GatewayReleaseAttemptPO(
        /**
         * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int attemptNo,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 startedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by started at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant startedAt,
        /**
         * 中文说明：保存 completedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completed at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant completedAt,
        /**
         * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorCode,
        /**
         * 中文说明：保存 error消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error message; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorMessage,
        /**
         * 中文说明：保存 targets 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayReleaseTargetPO>}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by targets; its type is {@code List<GatewayReleaseTargetPO>}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayReleaseTargetPO> targets
) {
}
