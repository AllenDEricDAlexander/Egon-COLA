package top.egon.cola.component.gateway.admin.release.domain.po;


import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayReleasePublicationPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayReleasePublicationPO相关的职责与边界。
 * English summary: {@code GatewayReleasePublicationPO} is an immutable data carrier in the current Gateway module; it owns the publication record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param releaseId 参数 发布Id；parameter release id。
 * @param attemptNo 参数 attemptNo；parameter attempt no。
 * @param phaseOrder 参数 phaseOrder；parameter phase order。
 * @param phaseType 参数 phaseType；parameter phase type。
 * @param configKey 参数 config键；parameter config key。
 * @param contentValue 参数 content值；parameter content value。
 * @param contentSha256 参数 contentSha256；parameter content sha256。
 * @param expectedVersion 参数 expectedVersion；parameter expected version。
 * @param changeId 参数 changeId；parameter change id。
 * @param ddcTargetVersion 参数 ddcTargetVersion；parameter ddc target version。
 * @param status 参数 status；parameter status。
 * @param errorCode 参数 errorCode；parameter error code。
 * @param errorMessage 参数 error消息；parameter error message。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 */
public record GatewayReleasePublicationPO(
        /**
         * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String releaseId,
        /**
         * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int attemptNo,
        /**
         * 中文说明：保存 phaseOrder 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by phase order; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int phaseOrder,
        /**
         * 中文说明：保存 phaseType 对应的状态、依赖或配置值；字段类型为 {@code GatewayPublicationPhaseEnum}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by phase type; its type is {@code GatewayPublicationPhaseEnum}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayPublicationPhaseEnum phaseType,
        /**
         * 中文说明：保存 config键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by config key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String configKey,
        /**
         * 中文说明：保存 content值 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content value; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String contentValue,
        /**
         * 中文说明：保存 contentSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String contentSha256,
        /**
         * 中文说明：保存 expectedVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected version; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long expectedVersion,
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 ddcTargetVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ddc target version; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long ddcTargetVersion,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code GatewayPublicationStatusEnum}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code GatewayPublicationStatusEnum}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayPublicationStatusEnum status,
        /**
         * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorCode,
        /**
         * 中文说明：保存 error消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error message; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorMessage,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt
) {
}
