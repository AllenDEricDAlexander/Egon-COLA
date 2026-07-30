package top.egon.cola.component.gateway.core.http;

public final class GatewayRequestRejectedException extends RuntimeException {

    private final String code;

    private final int status;

    public GatewayRequestRejectedException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
