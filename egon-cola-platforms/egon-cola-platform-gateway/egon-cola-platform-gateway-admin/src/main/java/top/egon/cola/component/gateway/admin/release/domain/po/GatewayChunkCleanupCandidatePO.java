package top.egon.cola.component.gateway.admin.release.domain.po;


/**
 * 中文说明：{@code GatewayChunkCleanupCandidatePO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayChunkCleanupCandidatePO相关的职责与边界。
 * English summary: {@code GatewayChunkCleanupCandidatePO} is an immutable data carrier in the current Gateway module; it owns the chunk cleanup candidate-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param changeId 参数 changeId；parameter change id。
 * @param releaseId 参数 发布Id；parameter release id。
 * @param appCode 参数 appCode；parameter app code。
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param configKey 参数 config键；parameter config key。
 * @param targetVersion 参数 targetVersion；parameter target version。
 */
public record GatewayChunkCleanupCandidatePO(
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String releaseId,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 config键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by config key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String configKey,
        /**
         * 中文说明：保存 targetVersion 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by target version; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long targetVersion
) {
}
