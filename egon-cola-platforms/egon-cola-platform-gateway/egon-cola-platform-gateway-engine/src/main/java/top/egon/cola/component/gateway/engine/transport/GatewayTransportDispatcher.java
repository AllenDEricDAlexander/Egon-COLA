package top.egon.cola.component.gateway.engine.transport;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.proxy.GatewayHttpProxyContext;
import top.egon.cola.component.gateway.engine.http.proxy.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketProxyContext;

import java.util.Objects;

/**
 * Invocation-stage transport dispatcher. Route, security, governance, and
 * provider selection remain responsibilities of the existing filter chain.
 */
public final class GatewayTransportDispatcher {

    private final GatewayHttpProxyStrategySelector httpStrategies;

    private final GatewayWebSocketProxy webSocketProxy;

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

    public Mono<GatewayOutboundHttpResponse> dispatchHttp(
            GatewayHttpProxyContext context) {
        Objects.requireNonNull(context, "context");
        return httpStrategies.select(context.policy().requestBodyMode())
                .proxy(context);
    }

    public Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            GatewayWebSocketProxyContext context) {
        return webSocketProxy.prepare(
                Objects.requireNonNull(context, "context")
        );
    }

    public Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        return webSocketProxy.bridge(
                Objects.requireNonNull(upstream, "upstream"),
                Objects.requireNonNull(downstream, "downstream")
        );
    }
}
