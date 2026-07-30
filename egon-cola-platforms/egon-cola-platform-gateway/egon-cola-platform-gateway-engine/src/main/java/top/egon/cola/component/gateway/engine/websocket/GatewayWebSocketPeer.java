package top.egon.cola.component.gateway.engine.websocket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Netty-free bidirectional WebSocket peer port.
 */
public interface GatewayWebSocketPeer {

    Flux<GatewayWebSocketFrame> receive();

    Mono<Void> send(Flux<GatewayWebSocketFrame> frames);

    Mono<Void> sendClose(GatewayWebSocketCloseStatus status);

    void dispose();

    boolean disposed();
}
