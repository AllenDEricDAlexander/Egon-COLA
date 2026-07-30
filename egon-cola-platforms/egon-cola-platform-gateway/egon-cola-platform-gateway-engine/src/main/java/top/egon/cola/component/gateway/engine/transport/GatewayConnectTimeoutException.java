package top.egon.cola.component.gateway.engine.transport;

public final class GatewayConnectTimeoutException
        extends GatewayTransportTimeoutException {

    GatewayConnectTimeoutException() {
        super("GATEWAY_CONNECT_TIMEOUT", "upstream connect timed out");
    }
}
