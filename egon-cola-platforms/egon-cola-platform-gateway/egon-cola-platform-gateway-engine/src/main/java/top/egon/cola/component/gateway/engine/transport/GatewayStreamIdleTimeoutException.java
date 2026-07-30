package top.egon.cola.component.gateway.engine.transport;

import java.util.Objects;

public final class GatewayStreamIdleTimeoutException
        extends GatewayTransportTimeoutException {

    private final GatewayStreamDirection direction;

    GatewayStreamIdleTimeoutException(GatewayStreamDirection direction) {
        super(
                "GATEWAY_" + direction + "_STREAM_IDLE_TIMEOUT",
                direction.name().toLowerCase() + " stream timed out"
        );
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public GatewayStreamDirection direction() {
        return direction;
    }
}
