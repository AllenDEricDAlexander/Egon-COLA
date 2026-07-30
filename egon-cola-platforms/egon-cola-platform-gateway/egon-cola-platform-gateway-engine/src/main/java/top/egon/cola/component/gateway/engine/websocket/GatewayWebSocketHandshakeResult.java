package top.egon.cola.component.gateway.engine.websocket;

import java.util.Objects;

public sealed interface GatewayWebSocketHandshakeResult
        permits GatewayWebSocketHandshakeResult.Accepted,
        GatewayWebSocketHandshakeResult.Rejected {

    static Accepted accepted(GatewayPreparedWebSocketSession session) {
        return new Accepted(session);
    }

    public static Rejected rejected(
            int httpStatus,
            String errorCode,
            String message) {
        return new Rejected(httpStatus, errorCode, message);
    }

    record Accepted(GatewayPreparedWebSocketSession session)
            implements GatewayWebSocketHandshakeResult {

        public Accepted {
            session = Objects.requireNonNull(session, "session");
        }
    }

    record Rejected(
            int httpStatus,
            String errorCode,
            String message
    ) implements GatewayWebSocketHandshakeResult {

        public Rejected {
            if (httpStatus < 400 || httpStatus > 599) {
                throw new IllegalArgumentException(
                        "rejected handshake requires 4xx or 5xx status"
                );
            }
            if (errorCode == null || errorCode.isBlank()) {
                throw new IllegalArgumentException(
                        "errorCode is required"
                );
            }
            message = message == null ? "" : message;
        }
    }
}
