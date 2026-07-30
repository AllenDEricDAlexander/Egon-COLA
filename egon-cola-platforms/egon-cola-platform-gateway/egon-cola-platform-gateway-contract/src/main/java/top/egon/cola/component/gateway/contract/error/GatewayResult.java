package top.egon.cola.component.gateway.contract.error;

import java.util.Objects;
import java.util.Optional;

/**
 * Protocol-independent success or failure result.
 */
public final class GatewayResult {

    private static final GatewayResult SUCCESS = new GatewayResult(true, null);

    private final boolean successful;

    private final GatewayError error;

    private GatewayResult(boolean successful, GatewayError error) {
        this.successful = successful;
        this.error = error;
    }

    public static GatewayResult success() {
        return SUCCESS;
    }

    public static GatewayResult failure(GatewayError error) {
        return new GatewayResult(
                false,
                Objects.requireNonNull(error, "error")
        );
    }

    public boolean successful() {
        return successful;
    }

    public Optional<GatewayError> error() {
        return Optional.ofNullable(error);
    }
}
