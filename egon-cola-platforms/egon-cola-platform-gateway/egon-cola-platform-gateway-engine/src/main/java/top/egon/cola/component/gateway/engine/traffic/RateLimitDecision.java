package top.egon.cola.component.gateway.engine.traffic;

public record RateLimitDecision(
        boolean allowed,
        long remaining,
        long retryAfterMillis,
        long resetAtEpochMillis,
        boolean localFallback,
        boolean backendUnavailable
) {
}
