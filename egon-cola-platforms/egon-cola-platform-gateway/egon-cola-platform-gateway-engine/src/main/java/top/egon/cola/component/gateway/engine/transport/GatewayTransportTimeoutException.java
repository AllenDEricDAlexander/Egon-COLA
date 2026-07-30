package top.egon.cola.component.gateway.engine.transport;

/**
 * Base failure for a specifically identified transport timeout boundary.
 */
public abstract class GatewayTransportTimeoutException
        extends RuntimeException {

    private final String errorCode;

    protected GatewayTransportTimeoutException(
            String errorCode,
            String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
