package top.egon.cola.component.gateway.test.webflux;

import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class RealtimeWebSocketProbe {

    private final AtomicInteger sessions = new AtomicInteger();

    private final AtomicInteger textFrames = new AtomicInteger();

    private final AtomicInteger binaryFrames = new AtomicInteger();

    private final AtomicInteger pingFrames = new AtomicInteger();

    private final AtomicInteger pongFrames = new AtomicInteger();

    private final AtomicReference<CloseStatus> closeStatus =
            new AtomicReference<>();

    void sessionOpened() {
        sessions.incrementAndGet();
    }

    void frameReceived(WebSocketMessage.Type type) {
        switch (type) {
            case TEXT -> textFrames.incrementAndGet();
            case BINARY -> binaryFrames.incrementAndGet();
            case PING -> pingFrames.incrementAndGet();
            case PONG -> pongFrames.incrementAndGet();
            default -> throw new IllegalArgumentException(
                    "Unsupported WebSocket frame type: " + type
            );
        }
    }

    void sessionClosed(CloseStatus status) {
        closeStatus.set(status);
    }

    public Snapshot snapshot() {
        CloseStatus status = closeStatus.get();
        return new Snapshot(
                sessions.get(),
                textFrames.get(),
                binaryFrames.get(),
                pingFrames.get(),
                pongFrames.get(),
                status == null ? null : status.getCode()
        );
    }

    public record Snapshot(
            int sessions,
            int textFrames,
            int binaryFrames,
            int pingFrames,
            int pongFrames,
            Integer closeCode) {
    }
}
