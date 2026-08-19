package top.egon.cola.component.gateway.engine.rpc.domain;

/**
 * 中文说明：{@code RpcGatewaySlotProperties} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Rpc网关槽位Properties相关的职责与边界。
 * English summary: {@code RpcGatewaySlotProperties} is an immutable data carrier in the current Gateway module; it owns the rpc gateway slot properties-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param enabled 参数 enabled；parameter enabled。
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param instanceId 参数 instanceId；parameter instance id。
 * @param advertisedHost 参数 advertisedHost；parameter advertised host。
 * @param serviceName 参数 服务Name；parameter service name。
 * @param group 参数 group；parameter group。
 * @param version 参数 version；parameter version。
 * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
 * @param gatewayVersion 参数 网关Version；parameter gateway version。
 * @param rpcRuntimeVersion 参数 rpc运行时Version；parameter rpc runtime version。
 * @param secure 参数 secure；parameter secure。
 * @param leaseSeconds 参数 租约Seconds；parameter lease seconds。
 * @param heartbeatIntervalSeconds 参数 heartbeatIntervalSeconds；parameter heartbeat interval seconds。
 */
public record RpcGatewaySlotProperties(
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String instanceId,
        /**
         * 中文说明：保存 advertisedHost 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by advertised host; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String advertisedHost,
        /**
         * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceName,
        /**
         * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String group,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 网关GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group code; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupCode,
        /**
         * 中文说明：保存 网关Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway version; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayVersion,
        /**
         * 中文说明：保存 rpc运行时Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rpc runtime version; its type is {@code String}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        String rpcRuntimeVersion,
        /**
         * 中文说明：保存 secure 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by secure; its type is {@code boolean}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean secure,
        /**
         * 中文说明：保存 租约Seconds 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease seconds; its type is {@code int}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int leaseSeconds,
        /**
         * 中文说明：保存 heartbeatIntervalSeconds 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcGatewaySlotProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by heartbeat interval seconds; its type is {@code int}, and {@code RpcGatewaySlotProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int heartbeatIntervalSeconds
) {

    /**
     * 中文说明：创建 {@code RpcGatewaySlotProperties} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewaySlotProperties} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param enabled 参数 enabled；parameter enabled。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param advertisedHost 参数 advertisedHost；parameter advertised host。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
     * @param gatewayVersion 参数 网关Version；parameter gateway version。
     * @param rpcRuntimeVersion 参数 rpc运行时Version；parameter rpc runtime version。
     * @param leaseSeconds 参数 租约Seconds；parameter lease seconds。
     * @param heartbeatIntervalSeconds 参数 heartbeatIntervalSeconds；parameter heartbeat interval seconds。
     */
    public RpcGatewaySlotProperties(
            boolean enabled,
            String env,
            String namespace,
            String instanceId,
            String advertisedHost,
            String serviceName,
            String group,
            String version,
            String gatewayGroupCode,
            String gatewayVersion,
            String rpcRuntimeVersion,
            int leaseSeconds,
            int heartbeatIntervalSeconds) {
        this(
                enabled,
                env,
                namespace,
                instanceId,
                advertisedHost,
                serviceName,
                group,
                version,
                gatewayGroupCode,
                gatewayVersion,
                rpcRuntimeVersion,
                false,
                leaseSeconds,
                heartbeatIntervalSeconds
        );
    }

    /**
     * 中文说明：创建 {@code RpcGatewaySlotProperties} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewaySlotProperties} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param enabled 参数 enabled；parameter enabled。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param advertisedHost 参数 advertisedHost；parameter advertised host。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
     * @param gatewayVersion 参数 网关Version；parameter gateway version。
     * @param rpcRuntimeVersion 参数 rpc运行时Version；parameter rpc runtime version。
     * @param secure 参数 secure；parameter secure。
     * @param leaseSeconds 参数 租约Seconds；parameter lease seconds。
     * @param heartbeatIntervalSeconds 参数 heartbeatIntervalSeconds；parameter heartbeat interval seconds。
     */
    public RpcGatewaySlotProperties {
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        instanceId = required(instanceId, "instanceId");
        advertisedHost = required(advertisedHost, "advertisedHost");
        serviceName = required(serviceName, "serviceName");
        group = required(group, "group");
        version = required(version, "version");
        gatewayGroupCode = required(gatewayGroupCode, "gatewayGroupCode");
        gatewayVersion = required(gatewayVersion, "gatewayVersion");
        rpcRuntimeVersion = required(rpcRuntimeVersion, "rpcRuntimeVersion");
        if (leaseSeconds <= 0
                || heartbeatIntervalSeconds <= 0
                || heartbeatIntervalSeconds >= leaseSeconds) {
            throw new IllegalArgumentException(
                    "heartbeat interval must be less than lease"
            );
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code RpcGatewaySlotProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code RpcGatewaySlotProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotProperties.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
