package top.egon.cola.component.gateway.admin.catalog.domain.dto;


import java.util.List;

/**
 * 中文说明：{@code GatewayManualMetadataDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual元数据相关的职责与边界。
 * English summary: {@code GatewayManualMetadataDTO} is an immutable data carrier in the current Gateway module; it owns the manual metadata-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param summary 参数 summary；parameter summary。
 * @param tags 参数 tags；parameter tags。
 * @param owner 参数 owner；parameter owner。
 */
public record GatewayManualMetadataDTO(
        /**
         * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String summary,
        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<String> tags,
        /**
         * 中文说明：保存 owner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by owner; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String owner
) {
}
