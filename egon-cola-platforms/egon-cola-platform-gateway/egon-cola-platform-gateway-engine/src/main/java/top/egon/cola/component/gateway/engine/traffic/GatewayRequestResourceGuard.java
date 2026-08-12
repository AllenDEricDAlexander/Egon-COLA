package top.egon.cola.component.gateway.engine.traffic;

import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayRequestResourceGuard} 是类型，位于当前 Gateway 模块的相关包中，负责网关请求资源Guard相关的职责与边界。
 * English summary: {@code GatewayRequestResourceGuard} is a type in the current Gateway module; it owns the gateway request resource guard-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRequestResourceGuard {

    /**
     * 中文说明：保存 limits 对应的状态、依赖或配置值；字段类型为 {@code GatewayResourceLimits}，由 {@code GatewayRequestResourceGuard} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by limits; its type is {@code GatewayResourceLimits}, and {@code GatewayRequestResourceGuard} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRequestResourceGuard} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRequestResourceGuard}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayResourceLimits limits;

    /**
     * 中文说明：创建 {@code GatewayRequestResourceGuard} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRequestResourceGuard} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param limits 参数 limits；parameter limits。
     */
    public GatewayRequestResourceGuard(GatewayResourceLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayRequestResourceGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayRequestResourceGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRequestResourceGuard.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     */
    public void validate(NormalizedHttpRequest request) {
        Objects.requireNonNull(request, "request");
        int queryCount = request.rawQuery().isEmpty()
                ? 0
                : request.rawQuery().split("&", -1).length;
        int pathSegments = request.normalizedPath().equals("/")
                ? 0
                : request.normalizedPath().substring(1).split("/", -1).length;
        int metadataBytes = request.headers().entrySet().stream()
                .mapToInt(entry -> entry.getKey().getBytes(
                        StandardCharsets.UTF_8
                ).length + entry.getValue().stream().mapToInt(
                        value -> value.getBytes(StandardCharsets.UTF_8).length
                ).sum())
                .sum();
        if (queryCount > limits.maximumQueryParameters()
                || pathSegments > limits.maximumPathSegments()
                || metadataBytes > limits.maximumMetadataBytes()) {
            throw new GatewayRequestRejectedException(
                    "GATEWAY_REQUEST_LIMIT_EXCEEDED",
                    413,
                    "request resource limit exceeded"
            );
        }
    }
}
