package top.egon.cola.component.gateway.engine.http.proxy;

import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.engine.http.GatewayBodySizeLimiter;
import top.egon.cola.component.gateway.engine.http.GatewayHttpFlushMode;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 中文说明：{@code GatewayHttpResponseSemantics} 是类型，位于当前 Gateway 模块的相关包中，负责网关Http响应Semantics相关的职责与边界。
 * English summary: {@code GatewayHttpResponseSemantics} is a type in the current Gateway module; it owns the gateway http response semantics-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
final class GatewayHttpResponseSemantics {

    /**
     * 中文说明：保存 limiter 对应的状态、依赖或配置值；字段类型为 {@code GatewayBodySizeLimiter}，由 {@code GatewayHttpResponseSemantics} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by limiter; its type is {@code GatewayBodySizeLimiter}, and {@code GatewayHttpResponseSemantics} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpResponseSemantics} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpResponseSemantics}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayBodySizeLimiter limiter;

    /**
     * 中文说明：创建 {@code GatewayHttpResponseSemantics} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpResponseSemantics} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param limiter 参数 limiter；parameter limiter。
     */
    GatewayHttpResponseSemantics(GatewayBodySizeLimiter limiter) {
        this.limiter = limiter;
    }

    /**
     * 中文说明：执行 apply 操作；该方法是 {@code GatewayHttpResponseSemantics} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the apply operation; this method is the invocation entry point on {@code GatewayHttpResponseSemantics} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpResponseSemantics.apply(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param context 参数 context；parameter context。
     * @return 返回 apply 的处理结果；returns the result of the operation.
     */
    GatewayOutboundHttpResponse apply(
            GatewayOutboundHttpResponse response,
            GatewayHttpProxyContext context) {
        GatewayOutboundHttpResponse limited = context.policy()
                .maxResponseBodyBytes()
                .isPresent()
                ? limiter.limitResponse(
                        response,
                        context.policy().maxResponseBodyBytes().getAsLong()
                )
                : response;
        GatewayTransportResponseMode mode = context.policy().responseMode();
        boolean sse = mode == GatewayTransportResponseMode.SSE
                || (mode == GatewayTransportResponseMode.AUTO_STREAM
                && eventStream(limited.headers()));
        if (!sse) {
            return limited;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        limited.headers().forEach((name, values) -> {
            if (!"content-length".equalsIgnoreCase(name)) {
                headers.put(name.toLowerCase(Locale.ROOT), values);
            }
        });
        headers.put("cache-control", List.of("no-cache, no-transform"));
        headers.put("x-accel-buffering", List.of("no"));
        return limited.withHeadersAndBody(headers, limited.body())
                .withFlushMode(GatewayHttpFlushMode.PER_BUFFER);
    }

    /**
     * 中文说明：执行 事件Stream 操作；该方法是 {@code GatewayHttpResponseSemantics} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the event stream operation; this method is the invocation entry point on {@code GatewayHttpResponseSemantics} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpResponseSemantics.eventStream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 事件Stream 的处理结果；returns the result of the operation.
     */
    private boolean eventStream(Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .filter(entry -> "content-type".equalsIgnoreCase(
                        entry.getKey()
                ))
                .flatMap(entry -> entry.getValue().stream())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.startsWith("text/event-stream"));
    }
}
