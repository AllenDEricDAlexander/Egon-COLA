package top.egon.cola.component.gateway.engine.discovery;

import java.time.Duration;
import java.util.Objects;

public record PassiveHealthPolicy(
        int consecutiveFailureThreshold,
        int minimumSamples,
        double failureRateThreshold,
        Duration window,
        Duration baseEjectionDuration,
        Duration maximumEjectionDuration
) {

    public PassiveHealthPolicy {
        if (consecutiveFailureThreshold < 1 || minimumSamples < 1) {
            throw new IllegalArgumentException(
                    "passive health thresholds must be positive"
            );
        }
        if (failureRateThreshold <= 0 || failureRateThreshold > 1) {
            throw new IllegalArgumentException(
                    "failureRateThreshold must be in (0, 1]"
            );
        }
        window = positive(window, "window");
        baseEjectionDuration = positive(
                baseEjectionDuration,
                "baseEjectionDuration"
        );
        maximumEjectionDuration = positive(
                maximumEjectionDuration,
                "maximumEjectionDuration"
        );
        if (maximumEjectionDuration.compareTo(baseEjectionDuration) < 0) {
            throw new IllegalArgumentException(
                    "maximum ejection duration must not be shorter than base"
            );
        }
    }

    public static PassiveHealthPolicy defaults() {
        return new PassiveHealthPolicy(
                3,
                20,
                0.5,
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofMinutes(1)
        );
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
