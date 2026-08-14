package top.egon.cola.component.gateway.admin.runtime.domain.dto;


import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;

import java.util.Locale;

/**
 * 中文说明：{@code GatewayProviderQueryDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方Query相关的职责与边界。
 * English summary: {@code GatewayProviderQueryDTO} is an immutable data carrier in the current Gateway module; it owns the provider query-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param appCode 参数 appCode；parameter app code。
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param serviceKind 参数 服务Kind；parameter service kind。
 * @param protocol 参数 protocol；parameter protocol。
 * @param serviceName 参数 服务Name；parameter service name。
 * @param group 参数 group；parameter group。
 * @param version 参数 version；parameter version。
 */
public record GatewayProviderQueryDTO(
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 服务Kind 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service kind; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceKind,
        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String protocol,
        /**
         * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceName,
        /**
         * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String group,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version
) {

    /**
     * 中文说明：执行 ddc 操作；该方法是 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ddc operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO.ddc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 ddc 的处理结果；returns the result of the operation.
     */
    public DdcManagementServiceQuery ddc() {
        String ddcProtocol = normalize(protocol);
        if (ddcProtocol != null) {
            ddcProtocol = ddcProtocol.toLowerCase(Locale.ROOT);
        }
        if ("rpc".equals(ddcProtocol)) {
            ddcProtocol = "grpc";
        }
        String ddcServiceKind = normalize(serviceKind);
        if (ddcServiceKind == null && ddcProtocol != null) {
            ddcServiceKind = switch (ddcProtocol) {
                case "http", "https" -> "HTTP_PROVIDER";
                case "grpc" -> "RPC_PROVIDER";
                default -> throw new IllegalArgumentException(
                        "serviceKind is required for protocol "
                                + protocol
                );
            };
        } else if (ddcServiceKind != null) {
            ddcServiceKind = ddcServiceKind.toUpperCase(Locale.ROOT);
        }
        return new DdcManagementServiceQuery(
                bizCode,
                namespace,
                env,
                appCode,
                ddcServiceKind,
                ddcProtocol,
                serviceName,
                group,
                version
        );
    }

    /**
     * 中文说明：执行 normalize 操作；该方法是 {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalize operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO.normalize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 normalize 的处理结果；returns the result of the operation.
     */
    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
