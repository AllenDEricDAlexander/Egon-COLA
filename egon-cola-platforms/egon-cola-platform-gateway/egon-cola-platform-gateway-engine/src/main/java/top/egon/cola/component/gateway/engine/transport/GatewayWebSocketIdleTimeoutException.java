package top.egon.cola.component.gateway.engine.transport;

public final class GatewayWebSocketIdleTimeoutException
        extends GatewayTransportTimeoutException {

    GatewayWebSocketIdleTimeoutException() {
        super(
                "GATEWAY_WEBSOCKET_IDLE_TIMEOUT",
                "websocket stream timed out"
        );
    }
}
