package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.proxy.domain.GatewayHttpProxyContext;
import top.egon.cola.component.gateway.engine.http.proxy.service.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketProxyContext;

import java.util.Objects;

/**
 * Invocation-stage transport dispatcher. Route, security, governance, and
 * provider selection remain responsibilities of the existing filter chain.
 * 补充说明 / Supplementary summary: {@code GatewayTransportDispatcher} 是分发器，位于当前 Gateway 模块的相关包中，负责网关传输分发器相关的职责与边界。
 * English supplement: {@code GatewayTransportDispatcher} is a gateway transport dispatcher dispatcher in the current Gateway module; it owns the gateway transport dispatcher-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayTransportDispatcher {

    /**
     * 中文说明：保存 httpStrategies 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpProxyStrategySelector}，由 {@code GatewayTransportDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http strategies; its type is {@code GatewayHttpProxyStrategySelector}, and {@code GatewayTransportDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTransportDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpProxyStrategySelector httpStrategies;

    /**
     * 中文说明：保存 webSocket代理 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketProxy}，由 {@code GatewayTransportDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by web socket proxy; its type is {@code GatewayWebSocketProxy}, and {@code GatewayTransportDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTransportDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayWebSocketProxy webSocketProxy;

    /**
     * 中文说明：创建 {@code GatewayTransportDispatcher} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTransportDispatcher} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param httpStrategies 参数 httpStrategies；parameter http strategies。
     * @param webSocketProxy 参数 webSocket代理；parameter web socket proxy。
     */
    public GatewayTransportDispatcher(
            GatewayHttpProxyStrategySelector httpStrategies,
            GatewayWebSocketProxy webSocketProxy) {
        this.httpStrategies = Objects.requireNonNull(
                httpStrategies,
                "httpStrategies"
        );
        this.webSocketProxy = Objects.requireNonNull(
                webSocketProxy,
                "webSocketProxy"
        );
    }

    /**
     * 中文说明：执行 dispatchHttp 操作；该方法是 {@code GatewayTransportDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispatch http operation; this method is the invocation entry point on {@code GatewayTransportDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportDispatcher.dispatchHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 dispatchHttp 的处理结果；returns the result of the operation.
     */
    public Mono<GatewayOutboundHttpResponse> dispatchHttp(
            GatewayHttpProxyContext context) {
        Objects.requireNonNull(context, "context");
        return httpStrategies.select(context.policy().requestBodyMode())
                .proxy(context);
    }

    /**
     * 中文说明：执行 prepareWebSocket 操作；该方法是 {@code GatewayTransportDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare web socket operation; this method is the invocation entry point on {@code GatewayTransportDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportDispatcher.prepareWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 prepareWebSocket 的处理结果；returns the result of the operation.
     */
    public Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            GatewayWebSocketProxyContext context) {
        return webSocketProxy.prepare(
                Objects.requireNonNull(context, "context")
        );
    }

    /**
     * 中文说明：执行 bridgeWebSocket 操作；该方法是 {@code GatewayTransportDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bridge web socket operation; this method is the invocation entry point on {@code GatewayTransportDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportDispatcher.bridgeWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @return 返回 bridgeWebSocket 的处理结果；returns the result of the operation.
     */
    public Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        return webSocketProxy.bridge(
                Objects.requireNonNull(upstream, "upstream"),
                Objects.requireNonNull(downstream, "downstream")
        );
    }
}
