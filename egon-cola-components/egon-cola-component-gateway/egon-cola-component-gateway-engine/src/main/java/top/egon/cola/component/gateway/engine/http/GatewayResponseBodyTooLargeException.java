package top.egon.cola.component.gateway.engine.http;

public final class GatewayResponseBodyTooLargeException
        extends RuntimeException {

    private static final String CODE = "GATEWAY_RESPONSE_BODY_TOO_LARGE";

    public GatewayResponseBodyTooLargeException(String message) {
        super(message);
    }

    public String code() {
        return CODE;
    }
}
