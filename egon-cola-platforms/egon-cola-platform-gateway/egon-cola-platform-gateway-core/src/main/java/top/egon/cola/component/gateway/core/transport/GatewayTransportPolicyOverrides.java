package top.egon.cola.component.gateway.core.transport;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record GatewayTransportPolicyOverrides(
        OptionalLong maxRequestBodyBytes,
        OptionalLong maxResponseBodyBytes,
        Optional<Duration> totalTimeout
) {

    public GatewayTransportPolicyOverrides {
        maxRequestBodyBytes = positive(
                maxRequestBodyBytes,
                "maxRequestBodyBytes"
        );
        maxResponseBodyBytes = positive(
                maxResponseBodyBytes,
                "maxResponseBodyBytes"
        );
        totalTimeout = Objects.requireNonNull(totalTimeout, "totalTimeout");
        totalTimeout.ifPresent(timeout -> {
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException(
                        "totalTimeout must be positive"
                );
            }
        });
    }

    public static GatewayTransportPolicyOverrides none() {
        return new GatewayTransportPolicyOverrides(
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.empty()
        );
    }

    private static OptionalLong positive(
            OptionalLong value,
            String field) {
        Objects.requireNonNull(value, field);
        if (value.isPresent() && value.getAsLong() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
