package top.egon.cola.component.gateway.engine.common.traffic.domain;

/**
 * 中文说明：{@code GatewayTrafficRejectedException} 是异常类型，位于当前 Gateway 模块的相关包中，负责网关流量RejectedException相关的职责与边界。
 * English summary: {@code GatewayTrafficRejectedException} is a gateway traffic rejected exception exception in the current Gateway module; it owns the gateway traffic rejected exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayTrafficRejectedException
        extends RuntimeException {

    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficRejectedException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayTrafficRejectedException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficRejectedException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficRejectedException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String code;

    /**
     * 中文说明：保存 httpStatus 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayTrafficRejectedException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http status; its type is {@code int}, and {@code GatewayTrafficRejectedException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficRejectedException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficRejectedException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int httpStatus;

    /**
     * 中文说明：保存 rpcStatus 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficRejectedException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc status; its type is {@code String}, and {@code GatewayTrafficRejectedException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficRejectedException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficRejectedException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String rpcStatus;

    /**
     * 中文说明：保存 重试AfterMillis 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayTrafficRejectedException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by retry after millis; its type is {@code long}, and {@code GatewayTrafficRejectedException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficRejectedException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficRejectedException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long retryAfterMillis;

    /**
     * 中文说明：创建 {@code GatewayTrafficRejectedException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTrafficRejectedException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param code 参数 code；parameter code。
     * @param httpStatus 参数 httpStatus；parameter http status。
     * @param rpcStatus 参数 rpcStatus；parameter rpc status。
     * @param retryAfterMillis 参数 重试AfterMillis；parameter retry after millis。
     */
    public GatewayTrafficRejectedException(
            String code,
            int httpStatus,
            String rpcStatus,
            long retryAfterMillis) {
        super(code);
        this.code = code;
        this.httpStatus = httpStatus;
        this.rpcStatus = rpcStatus;
        this.retryAfterMillis = Math.max(0, retryAfterMillis);
    }

    /**
     * 中文说明：执行 code 操作；该方法是 {@code GatewayTrafficRejectedException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code operation; this method is the invocation entry point on {@code GatewayTrafficRejectedException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficRejectedException.code(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 code 的处理结果；returns the result of the operation.
     */
    public String code() {
        return code;
    }

    /**
     * 中文说明：执行 httpStatus 操作；该方法是 {@code GatewayTrafficRejectedException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http status operation; this method is the invocation entry point on {@code GatewayTrafficRejectedException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficRejectedException.httpStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 httpStatus 的处理结果；returns the result of the operation.
     */
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * 中文说明：执行 rpcStatus 操作；该方法是 {@code GatewayTrafficRejectedException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc status operation; this method is the invocation entry point on {@code GatewayTrafficRejectedException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficRejectedException.rpcStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 rpcStatus 的处理结果；returns the result of the operation.
     */
    public String rpcStatus() {
        return rpcStatus;
    }

    /**
     * 中文说明：执行 重试AfterMillis 操作；该方法是 {@code GatewayTrafficRejectedException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retry after millis operation; this method is the invocation entry point on {@code GatewayTrafficRejectedException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficRejectedException.retryAfterMillis(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 重试AfterMillis 的处理结果；returns the result of the operation.
     */
    public long retryAfterMillis() {
        return retryAfterMillis;
    }
}
