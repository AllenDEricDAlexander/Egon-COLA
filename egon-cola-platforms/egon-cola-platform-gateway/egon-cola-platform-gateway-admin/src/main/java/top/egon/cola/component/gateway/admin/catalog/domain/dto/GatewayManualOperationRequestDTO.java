package top.egon.cola.component.gateway.admin.catalog.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 中文说明：{@code GatewayManualOperationRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual操作请求相关的职责与边界。
 * English summary: {@code GatewayManualOperationRequestDTO} is an immutable data carrier in the current Gateway module; it owns the manual operation request-related responsibility and boundary.
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
public record GatewayManualOperationRequestDTO(
        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code top.egon.cola.component.gateway.admin.catalog.domain.enums.GatewayCatalogProtocolEnum}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code top.egon.cola.component.gateway.admin.catalog.domain.enums.GatewayCatalogProtocolEnum}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull top.egon.cola.component.gateway.admin.catalog.domain.enums.GatewayCatalogProtocolEnum protocol,
        /**
         * 中文说明：保存 http方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http method; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String httpMethod,
        /**
         * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String path,
        /**
         * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceName,
        /**
         * 中文说明：保存 full方法Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by full method name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String fullMethodName,
        /**
         * 中文说明：保存 提供方服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider service name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String providerServiceName,
        /**
         * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String group,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 传输 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by transport; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String transport,
        /**
         * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean externalAccessible,
        /**
         * 中文说明：保存 定义 对应的状态、依赖或配置值；字段类型为 {@code GatewayManualDefinitionRequestDTO}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition; its type is {@code GatewayManualDefinitionRequestDTO}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull GatewayManualDefinitionRequestDTO definition
) {

    /**
     * 中文说明：执行 command 操作；该方法是 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the command operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO.command(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 command 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO command() {
        return new top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO(
                protocol,
                httpMethod,
                path,
                serviceName,
                fullMethodName,
                providerServiceName,
                group,
                version,
                transport,
                externalAccessible,
                definition.definition()
        );
    }
}
