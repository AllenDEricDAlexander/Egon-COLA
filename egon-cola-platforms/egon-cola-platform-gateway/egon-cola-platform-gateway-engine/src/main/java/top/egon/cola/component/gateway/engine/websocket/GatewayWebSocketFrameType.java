package top.egon.cola.component.gateway.engine.websocket;

public enum GatewayWebSocketFrameType {
    TEXT,
    BINARY,
    CONTINUATION,
    PING,
    PONG,
    CLOSE
}
