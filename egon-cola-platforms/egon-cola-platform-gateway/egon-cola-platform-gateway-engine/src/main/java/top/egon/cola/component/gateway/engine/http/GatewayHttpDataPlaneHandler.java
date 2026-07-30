package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketPeer;

@FunctionalInterface
public interface GatewayHttpDataPlaneHandler {

    Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request
    );

    default Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        return Mono.just(GatewayWebSocketHandshakeResult.rejected(
                426,
                "GATEWAY_WEBSOCKET_NOT_SUPPORTED",
                "the selected data plane does not support WebSocket"
        ));
    }

    default Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        upstream.dispose();
        downstream.dispose();
        return Mono.error(new IllegalStateException(
                "GATEWAY_WEBSOCKET_NOT_SUPPORTED"
        ));
    }
}
