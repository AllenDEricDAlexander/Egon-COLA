package top.egon.cola.component.gateway.engine.transport;

public final class GatewayTotalTimeoutException
        extends GatewayTransportTimeoutException {

    GatewayTotalTimeoutException() {
        super("GATEWAY_TOTAL_TIMEOUT", "gateway request timed out");
    }
}
