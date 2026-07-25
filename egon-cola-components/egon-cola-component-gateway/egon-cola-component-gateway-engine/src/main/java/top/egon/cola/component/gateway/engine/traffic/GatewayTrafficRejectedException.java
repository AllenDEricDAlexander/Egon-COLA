package top.egon.cola.component.gateway.engine.traffic;

public final class GatewayTrafficRejectedException
        extends RuntimeException {

    private final String code;

    private final int httpStatus;

    private final String rpcStatus;

    private final long retryAfterMillis;

    public GatewayTrafficRejectedException(
            String code,
            int httpStatus,
            String rpcStatus,
            long retryAfterMillis) {
        super(code);
        this.code = code;
        this.httpStatus = httpStatus;
        this.rpcStatus = rpcStatus;
        this.retryAfterMillis = Math.max(0, retryAfterMillis);
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String rpcStatus() {
        return rpcStatus;
    }

    public long retryAfterMillis() {
        return retryAfterMillis;
    }
}
