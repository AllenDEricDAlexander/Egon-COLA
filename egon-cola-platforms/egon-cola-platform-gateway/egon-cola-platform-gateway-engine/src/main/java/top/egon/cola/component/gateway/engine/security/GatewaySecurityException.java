package top.egon.cola.component.gateway.engine.security;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewaySecurityException} 是异常类型，位于当前 Gateway 模块的相关包中，负责网关安全Exception相关的职责与边界。
 * English summary: {@code GatewaySecurityException} is a gateway security exception exception in the current Gateway module; it owns the gateway security exception-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewaySecurityException extends RuntimeException {

    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewaySecurityException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewaySecurityException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String code;

    /**
     * 中文说明：保存 httpStatus 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewaySecurityException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http status; its type is {@code int}, and {@code GatewaySecurityException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int httpStatus;

    /**
     * 中文说明：保存 rpcStatus 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewaySecurityException} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc status; its type is {@code String}, and {@code GatewaySecurityException} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityException}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String rpcStatus;

    private final Map<String, List<String>> responseHeaders;

    /**
     * 中文说明：创建 {@code GatewaySecurityException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewaySecurityException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param code 参数 code；parameter code。
     * @param httpStatus 参数 httpStatus；parameter http status。
     * @param rpcStatus 参数 rpcStatus；parameter rpc status。
     */
    public GatewaySecurityException(
            String code,
            int httpStatus,
            String rpcStatus) {
        this(code, httpStatus, rpcStatus, Map.of());
    }

    public GatewaySecurityException(
            String code,
            int httpStatus,
            String rpcStatus,
            Map<String, List<String>> responseHeaders) {
        super(code);
        this.code = code;
        this.httpStatus = httpStatus;
        this.rpcStatus = rpcStatus;
        this.responseHeaders = responseHeaders == null
                ? Map.of()
                : Map.copyOf(responseHeaders);
    }

    /**
     * 中文说明：执行 code 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the code operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.code(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 code 的处理结果；returns the result of the operation.
     */
    public String code() {
        return code;
    }

    /**
     * 中文说明：执行 httpStatus 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http status operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.httpStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 httpStatus 的处理结果；returns the result of the operation.
     */
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * 中文说明：执行 rpcStatus 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc status operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.rpcStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 rpcStatus 的处理结果；returns the result of the operation.
     */
    public String rpcStatus() {
        return rpcStatus;
    }

    public Map<String, List<String>> responseHeaders() {
        return responseHeaders;
    }

    /**
     * 中文说明：执行 凭证Invalid 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the credential invalid operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.credentialInvalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 凭证Invalid 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException credentialInvalid() {
        return new GatewaySecurityException(
                "GATEWAY_CREDENTIAL_INVALID",
                401,
                "UNAUTHENTICATED"
        );
    }

    /**
     * 中文说明：执行 authenticationRequired 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authentication required operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.authenticationRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 authenticationRequired 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException authenticationRequired() {
        return new GatewaySecurityException(
                "GATEWAY_AUTHENTICATION_REQUIRED",
                401,
                "UNAUTHENTICATED"
        );
    }

    /**
     * 中文说明：执行 authenticationFailed 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authentication failed operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.authenticationFailed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 authenticationFailed 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException authenticationFailed() {
        return new GatewaySecurityException(
                "GATEWAY_AUTHENTICATION_FAILED",
                401,
                "UNAUTHENTICATED"
        );
    }

    public static GatewaySecurityException authenticationFailed(
            Map<String, List<String>> responseHeaders) {
        return new GatewaySecurityException(
                "GATEWAY_AUTHENTICATION_FAILED",
                401,
                "UNAUTHENTICATED",
                responseHeaders);
    }

    /**
     * 中文说明：执行 授权Denied 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorization denied operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.authorizationDenied(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 授权Denied 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException authorizationDenied() {
        return new GatewaySecurityException(
                "GATEWAY_AUTHORIZATION_DENIED",
                403,
                "PERMISSION_DENIED"
        );
    }

    /**
     * 中文说明：执行 提供方超时 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider timeout operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.providerTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 提供方超时 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException providerTimeout() {
        return new GatewaySecurityException(
                "GATEWAY_SECURITY_PROVIDER_TIMEOUT",
                503,
                "UNAVAILABLE"
        );
    }

    /**
     * 中文说明：执行 提供方Error 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider error operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.providerError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 提供方Error 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException providerError() {
        return new GatewaySecurityException(
                "GATEWAY_SECURITY_PROVIDER_ERROR",
                503,
                "UNAVAILABLE"
        );
    }

    /**
     * 中文说明：执行 身份MappingFailed 操作；该方法是 {@code GatewaySecurityException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity mapping failed operation; this method is the invocation entry point on {@code GatewaySecurityException} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityException.identityMappingFailed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 身份MappingFailed 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityException identityMappingFailed() {
        return new GatewaySecurityException(
                "GATEWAY_IDENTITY_MAPPING_FAILED",
                500,
                "INTERNAL"
        );
    }
}
