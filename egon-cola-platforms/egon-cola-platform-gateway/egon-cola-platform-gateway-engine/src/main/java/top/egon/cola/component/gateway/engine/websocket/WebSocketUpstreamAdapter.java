package top.egon.cola.component.gateway.engine.websocket;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface WebSocketUpstreamAdapter {

    Mono<GatewayWebSocketHandshakeResult> prepare(
            GatewayWebSocketProxyContext context);
}
