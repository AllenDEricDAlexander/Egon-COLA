package top.egon.cola.component.yuheng.admin.openapi.client;

import java.util.Objects;

/**
 * Safe classified failure from the Provider OpenAPI acquisition boundary.
 *
 * <p>The message is deliberately operator-safe and never contains a token,
 * response body, full target URI or resolved address.</p>
 */
public class GatewayOpenApiFetchException extends RuntimeException {

    private final String errorCode;

    private final boolean retryable;

    /**
     * Creates a classified safe fetch failure.
     *
     * @param errorCode stable error category
     * @param retryable whether the caller may retry another same-build instance
     * @param message bounded safe operator message
     */
    public GatewayOpenApiFetchException(
            String errorCode,
            boolean retryable,
            String message) {
        super(Objects.requireNonNull(message, "message"));
        this.errorCode = required(errorCode);
        this.retryable = retryable;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }

    private static String required(String value) {
        String normalized = Objects.requireNonNull(value, "errorCode").trim();
        if (normalized.isEmpty() || normalized.length() > 128) {
            throw new IllegalArgumentException(
                    "errorCode must contain 1..128 characters"
            );
        }
        return normalized;
    }
}
