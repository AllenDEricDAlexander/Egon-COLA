package top.egon.cola.component.gateway.engine.traffic;

import java.time.Duration;
import java.util.Objects;

public record GatewayRetryPolicy(
        boolean enabled,
        int maxAttempts,
        Duration initialBackoff,
        Duration maximumBackoff,
        double multiplier,
        Duration minimumAttemptBudget
) {

    public GatewayRetryPolicy {
        if (maxAttempts < 1 || multiplier < 1) {
            throw new IllegalArgumentException("invalid retry bounds");
        }
        initialBackoff = nonNegative(initialBackoff, "initialBackoff");
        maximumBackoff = nonNegative(maximumBackoff, "maximumBackoff");
        minimumAttemptBudget = positive(
                minimumAttemptBudget,
                "minimumAttemptBudget"
        );
        if (maximumBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException(
                    "maximumBackoff must not be shorter than initial"
            );
        }
        if (!enabled && maxAttempts != 1) {
            throw new IllegalArgumentException(
                    "disabled retry policy must use one attempt"
            );
        }
    }

    public static GatewayRetryPolicy disabled() {
        return new GatewayRetryPolicy(
                false,
                1,
                Duration.ZERO,
                Duration.ZERO,
                1,
                Duration.ofMillis(1)
        );
    }

    Duration backoff(int completedAttempts) {
        double factor = Math.pow(multiplier, Math.max(0, completedAttempts - 1));
        long requested;
        try {
            requested = Math.multiplyExact(
                    initialBackoff.toNanos(),
                    Math.max(1L, Math.round(factor))
            );
        } catch (ArithmeticException overflow) {
            requested = maximumBackoff.toNanos();
        }
        return Duration.ofNanos(Math.min(
                requested,
                maximumBackoff.toNanos()
        ));
    }

    private static Duration nonNegative(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative()) {
            throw new IllegalArgumentException(
                    field + " must not be negative"
            );
        }
        return value;
    }

    private static Duration positive(Duration value, String field) {
        nonNegative(value, field);
        if (value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
