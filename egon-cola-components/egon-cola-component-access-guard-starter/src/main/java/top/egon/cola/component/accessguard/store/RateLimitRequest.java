package top.egon.cola.component.accessguard.store;

import java.time.Duration;

public record RateLimitRequest(
        String ruleId,
        String stateVersion,
        String keyHash,
        long capacity,
        long refillTokens,
        Duration refillPeriod,
        long requestedTokens
) {

    public RateLimitRequest {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        if (stateVersion == null || stateVersion.isBlank()) {
            throw new IllegalArgumentException("stateVersion must not be blank");
        }
        if (keyHash == null || !keyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
        if (capacity <= 0 || refillTokens <= 0 || requestedTokens <= 0 || requestedTokens > capacity) {
            throw new IllegalArgumentException("token values must be positive and requestedTokens must not exceed capacity");
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("refillPeriod must be positive");
        }
        ruleId = ruleId.trim();
        stateVersion = stateVersion.trim();
    }

    @Override
    public String toString() {
        return "RateLimitRequest[ruleId=" + ruleId + ", stateVersion=" + stateVersion
                + ", keyHash=<redacted>, capacity=" + capacity + ", refillTokens=" + refillTokens
                + ", refillPeriod=" + refillPeriod + ", requestedTokens=" + requestedTokens + "]";
    }
}
