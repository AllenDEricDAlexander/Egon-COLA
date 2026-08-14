package top.egon.cola.component.gateway.admin.catalog.domain.dto;


import jakarta.validation.constraints.NotBlank;

/**
 * 中文说明：{@code GatewayManualInterfaceGroupRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual接口Group请求相关的职责与边界。
 * English summary: {@code GatewayManualInterfaceGroupRequestDTO} is an immutable data carrier in the current Gateway module; it owns the manual interface group request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param businessCode 参数 businessCode；parameter business code。
 * @param businessName 参数 businessName；parameter business name。
 * @param entityCode 参数 entityCode；parameter entity code。
 * @param entityName 参数 entityName；parameter entity name。
 * @param interfaceGroupCode 参数 接口GroupCode；parameter interface group code。
 * @param interfaceGroupName 参数 接口GroupName；parameter interface group name。
 * @param className 参数 className；parameter class name。
 * @param description 参数 description；parameter description。
 */
public record GatewayManualInterfaceGroupRequestDTO(
        /**
         * 中文说明：保存 businessCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by business code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String businessCode,
        /**
         * 中文说明：保存 businessName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by business name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String businessName,
        /**
         * 中文说明：保存 entityCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by entity code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String entityCode,
        /**
         * 中文说明：保存 entityName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by entity name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String entityName,
        /**
         * 中文说明：保存 接口GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interface group code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String interfaceGroupCode,
        /**
         * 中文说明：保存 接口GroupName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interface group name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String interfaceGroupName,
        /**
         * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String className,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description
) {
}
