package top.egon.cola.component.gateway.core.transport;

import java.time.Duration;
import java.util.Objects;

public record GatewayTransportSafetyLimits(
        long maxRequestBodyBytes,
        Duration maxConnectTimeout,
        Duration maxResponseHeaderTimeout,
        Duration maxStreamIdleTimeout,
        Duration maxTotalTimeout,
        Duration maxWebsocketIdleTimeout,
        long maxWebsocketFrameBytes
) {

    private static final long MIB = 1024L * 1024L;

    public GatewayTransportSafetyLimits {
        positive(maxRequestBodyBytes, "maxRequestBodyBytes");
        maxConnectTimeout = positive(
                maxConnectTimeout,
                "maxConnectTimeout"
        );
        maxResponseHeaderTimeout = positive(
                maxResponseHeaderTimeout,
                "maxResponseHeaderTimeout"
        );
        maxStreamIdleTimeout = positive(
                maxStreamIdleTimeout,
                "maxStreamIdleTimeout"
        );
        maxTotalTimeout = positive(maxTotalTimeout, "maxTotalTimeout");
        maxWebsocketIdleTimeout = positive(
                maxWebsocketIdleTimeout,
                "maxWebsocketIdleTimeout"
        );
        positive(maxWebsocketFrameBytes, "maxWebsocketFrameBytes");
    }

    public static GatewayTransportSafetyLimits specDefaults() {
        return new GatewayTransportSafetyLimits(
                1024L * MIB,
                Duration.ofSeconds(60),
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofHours(2),
                Duration.ofHours(2),
                64L * MIB
        );
    }

    private static long positive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
