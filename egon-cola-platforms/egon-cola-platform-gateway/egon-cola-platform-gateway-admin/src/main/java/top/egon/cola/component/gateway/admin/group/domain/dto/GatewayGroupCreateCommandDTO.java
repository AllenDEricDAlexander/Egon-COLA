package top.egon.cola.component.gateway.admin.group.domain.dto;


/**
 * 中文说明：{@code GatewayGroupCreateCommandDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Create网关Group相关的职责与边界。
 * English summary: {@code GatewayGroupCreateCommandDTO} is an immutable data carrier in the current Gateway module; it owns the create gateway group-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
 * @param displayName 参数 displayName；parameter display name。
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param description 参数 description；parameter description。
 */
public record GatewayGroupCreateCommandDTO(
        /**
         * 中文说明：保存 网关GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupCode,
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description
) {
}
