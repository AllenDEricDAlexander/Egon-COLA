package top.egon.cola.component.gateway.admin.group.domain.dto;


/**
 * 中文说明：{@code GatewayGroupUpdateCommandDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Update网关Group相关的职责与边界。
 * English summary: {@code GatewayGroupUpdateCommandDTO} is an immutable data carrier in the current Gateway module; it owns the update gateway group-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param displayName 参数 displayName；parameter display name。
 * @param description 参数 description；parameter description。
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 */
public record GatewayGroupUpdateCommandDTO(
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedRevision
) {
}
