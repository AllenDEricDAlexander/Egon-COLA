package top.egon.cola.component.accessguard.store;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, long remainingTokens, Duration retryAfter) {

    public RateLimitDecision {
        if (remainingTokens < 0) {
            throw new IllegalArgumentException("remainingTokens must not be negative");
        }
        retryAfter = retryAfter == null ? Duration.ZERO : retryAfter;
        if (retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative");
        }
        if (allowed && !retryAfter.isZero()) {
            throw new IllegalArgumentException("allowed decisions must not have retryAfter");
        }
    }
}
