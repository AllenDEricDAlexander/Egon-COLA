package top.egon.cola.component.gateway.test.webflux;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public final class RealtimeWebSocketHandler implements WebSocketHandler {

    private final RealtimeWebSocketProbe probe;

    public RealtimeWebSocketHandler(RealtimeWebSocketProbe probe) {
        this.probe = probe;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        probe.sessionOpened();
        Mono<Void> echo = session.send(session.receive()
                .doOnNext(message -> probe.frameReceived(message.getType()))
                .map(WebSocketMessage::retain));
        Mono<Void> close = session.closeStatus()
                .doOnNext(probe::sessionClosed)
                .then();
        return Mono.when(echo, close);
    }
}
