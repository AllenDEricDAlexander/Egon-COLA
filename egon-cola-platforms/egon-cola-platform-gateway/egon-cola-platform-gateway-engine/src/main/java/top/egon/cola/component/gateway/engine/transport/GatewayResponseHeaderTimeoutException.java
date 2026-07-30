package top.egon.cola.component.gateway.engine.transport;

public final class GatewayResponseHeaderTimeoutException
        extends GatewayTransportTimeoutException {

    GatewayResponseHeaderTimeoutException() {
        super(
                "GATEWAY_RESPONSE_HEADER_TIMEOUT",
                "upstream response headers timed out"
        );
    }
}
