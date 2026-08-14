package top.egon.cola.component.gateway.admin.application.domain.vo;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayApplicationVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关ApplicationView相关的职责与边界。
 * English summary: {@code GatewayApplicationVO} is an immutable data carrier in the current Gateway module; it owns the gateway application view-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param applicationCode 参数 applicationCode；parameter application code。
 * @param displayName 参数 displayName；parameter display name。
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param description 参数 description；parameter description。
 * @param ddcMatched 参数 ddcMatched；parameter ddc matched。
 * @param revision 参数 revision；parameter revision。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 */
public record GatewayApplicationVO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String applicationCode,
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description,
        /**
         * 中文说明：保存 ddcMatched 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ddc matched; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean ddcMatched,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt
) {
}
