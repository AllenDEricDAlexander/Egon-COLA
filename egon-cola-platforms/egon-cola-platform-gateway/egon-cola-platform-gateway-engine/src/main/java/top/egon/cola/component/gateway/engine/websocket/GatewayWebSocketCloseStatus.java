package top.egon.cola.component.gateway.engine.websocket;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket close fact that distinguishes sendable wire codes from 1006.
 */
public record GatewayWebSocketCloseStatus(int code, String reason) {

    private static final int MAX_REASON_BYTES = 123;

    public GatewayWebSocketCloseStatus {
        reason = reason == null ? "" : reason;
        if (!valid(code)) {
            throw new IllegalArgumentException(
                    "invalid WebSocket close code: " + code
            );
        }
        if (reason.getBytes(StandardCharsets.UTF_8).length
                > MAX_REASON_BYTES) {
            throw new IllegalArgumentException(
                    "WebSocket close reason exceeds 123 UTF-8 bytes"
            );
        }
    }

    public static GatewayWebSocketCloseStatus abnormal() {
        return new GatewayWebSocketCloseStatus(1006, "abnormal closure");
    }

    public static GatewayWebSocketCloseStatus goingAway() {
        return new GatewayWebSocketCloseStatus(1001, "going away");
    }

    public static GatewayWebSocketCloseStatus frameTooLarge() {
        return new GatewayWebSocketCloseStatus(1009, "frame too large");
    }

    public boolean sendable() {
        return code != 1006;
    }

    private static boolean valid(int code) {
        return code == 1006
                || (code >= 1000
                && code <= 1014
                && code != 1004
                && code != 1005)
                || (code >= 3000 && code <= 4999);
    }
}
