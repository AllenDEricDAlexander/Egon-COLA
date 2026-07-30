package top.egon.cola.component.gateway.engine.http;

public final class GatewayRequestBodyTooLargeException
        extends RuntimeException {

    private static final String CODE = "GATEWAY_REQUEST_BODY_TOO_LARGE";

    public GatewayRequestBodyTooLargeException(String message) {
        super(message);
    }

    public String code() {
        return CODE;
    }
}
