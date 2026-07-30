package top.egon.cola.component.gateway.engine.http;

public final class GatewayCorsException extends RuntimeException {

    private static final String CODE = "GATEWAY_CORS_REJECTED";

    public GatewayCorsException(String message) {
        super(message);
    }

    public String code() {
        return CODE;
    }
}
