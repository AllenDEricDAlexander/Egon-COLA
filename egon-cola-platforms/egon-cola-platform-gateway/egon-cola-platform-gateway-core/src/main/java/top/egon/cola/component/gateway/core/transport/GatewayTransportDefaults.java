package top.egon.cola.component.gateway.core.transport;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record GatewayTransportDefaults(
        long maxRequestBodyBytes,
        OptionalLong maxResponseBodyBytes,
        Duration connectTimeout,
        Duration responseHeaderTimeout,
        Duration streamIdleTimeout,
        Optional<Duration> totalTimeout,
        boolean bodyLogEnabled,
        boolean retryAllowed
) {

    private static final long MIB = 1024L * 1024L;

    public GatewayTransportDefaults {
        positive(maxRequestBodyBytes, "maxRequestBodyBytes");
        maxResponseBodyBytes = Objects.requireNonNull(
                maxResponseBodyBytes,
                "maxResponseBodyBytes"
        );
        if (maxResponseBodyBytes.isPresent()) {
            positive(maxResponseBodyBytes.getAsLong(), "maxResponseBodyBytes");
        }
        connectTimeout = positive(connectTimeout, "connectTimeout");
        responseHeaderTimeout = positive(
                responseHeaderTimeout,
                "responseHeaderTimeout"
        );
        streamIdleTimeout = positive(
                streamIdleTimeout,
                "streamIdleTimeout"
        );
        totalTimeout = Objects.requireNonNull(totalTimeout, "totalTimeout");
        totalTimeout.ifPresent(timeout -> positive(timeout, "totalTimeout"));
    }

    public static GatewayTransportDefaults legacy() {
        return new GatewayTransportDefaults(
                2L * MIB,
                OptionalLong.of(4L * MIB),
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Optional.empty(),
                false,
                true
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
