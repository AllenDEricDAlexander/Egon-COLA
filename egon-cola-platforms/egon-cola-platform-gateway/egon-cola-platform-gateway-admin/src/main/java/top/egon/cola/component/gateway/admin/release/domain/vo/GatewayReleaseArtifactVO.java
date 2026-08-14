package top.egon.cola.component.gateway.admin.release.domain.vo;


/**
 * 中文说明：{@code GatewayReleaseArtifactVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品相关的职责与边界。
 * English summary: {@code GatewayReleaseArtifactVO} is an immutable data carrier in the current Gateway module; it owns the artifact-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param phaseType 参数 phaseType；parameter phase type。
 * @param configKey 参数 config键；parameter config key。
 * @param value 参数 值；parameter value。
 */
public record GatewayReleaseArtifactVO(
        /**
         * 中文说明：保存 phaseType 对应的状态、依赖或配置值；字段类型为 {@code top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by phase type; its type is {@code top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum phaseType,
        /**
         * 中文说明：保存 config键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by config key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String configKey,
        /**
         * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String value
) {
}
