package top.egon.cola.component.gateway.engine.websocket;

/**
 * Passive lifecycle observation port; it cannot influence forwarding.
 */
@FunctionalInterface
public interface GatewayWebSocketObserver {

    void observe(
            String transportMode,
            String commitPoint,
            String terminationReason);

    default void observeFrame(
            String direction,
            GatewayWebSocketFrameType frameType,
            long payloadBytes,
            boolean finalFragment) {
    }

    static GatewayWebSocketObserver noop() {
        return (transportMode, commitPoint, terminationReason) -> {
        };
    }
}
