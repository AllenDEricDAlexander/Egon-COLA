package top.egon.cola.component.gateway.admin.catalog.domain.dto;


import top.egon.cola.component.gateway.admin.catalog.domain.enums.GatewayCatalogProtocolEnum;

/**
 * 中文说明：{@code GatewayManualOperationDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual操作相关的职责与边界。
 * English summary: {@code GatewayManualOperationDTO} is an immutable data carrier in the current Gateway module; it owns the manual operation-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param protocol 参数 protocol；parameter protocol。
 * @param httpMethod 参数 http方法；parameter http method。
 * @param path 参数 path；parameter path。
 * @param serviceName 参数 服务Name；parameter service name。
 * @param fullMethodName 参数 full方法Name；parameter full method name。
 * @param providerServiceName 参数 提供方服务Name；parameter provider service name。
 * @param group 参数 group；parameter group。
 * @param version 参数 version；parameter version。
 * @param transport 参数 传输；parameter transport。
 * @param externalAccessible 参数 externalAccessible；parameter external accessible。
 * @param definition 参数 定义；parameter definition。
 */
public record GatewayManualOperationDTO(
        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogProtocolEnum}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code GatewayCatalogProtocolEnum}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayCatalogProtocolEnum protocol,
        /**
         * 中文说明：保存 http方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http method; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String httpMethod,
        /**
         * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String path,
        /**
         * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceName,
        /**
         * 中文说明：保存 full方法Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by full method name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String fullMethodName,
        /**
         * 中文说明：保存 提供方服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider service name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String providerServiceName,
        /**
         * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String group,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 传输 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by transport; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String transport,
        /**
         * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean externalAccessible,
        /**
         * 中文说明：保存 定义 对应的状态、依赖或配置值；字段类型为 {@code GatewayManualDefinitionDTO}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition; its type is {@code GatewayManualDefinitionDTO}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayManualDefinitionDTO definition
) {
}
