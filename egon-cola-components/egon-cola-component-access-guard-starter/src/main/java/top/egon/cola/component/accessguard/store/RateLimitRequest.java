package top.egon.cola.component.accessguard.store;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;

import java.time.Duration;

public record RateLimitRequest(
        String ruleId,
        String stateVersion,
        String keyHash,
        AdmissionConfig.RateLimitAlgorithm algorithm,
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
        algorithm = java.util.Objects.requireNonNull(algorithm, "algorithm");
        if (keyHash == null || !keyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
        if (capacity <= 0 || refillTokens <= 0 || requestedTokens <= 0 || requestedTokens > capacity) {
            throw new IllegalArgumentException("token values must be positive and requestedTokens must not exceed capacity");
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("refillPeriod must be positive");
        }
        if (algorithm == AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW
                && requestedTokens != 1) {
            throw new IllegalArgumentException(
                    "SLIDING_WINDOW requires requestedTokens=1");
        }
        if (algorithm == AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW
                && capacity > 100_000) {
            throw new IllegalArgumentException(
                    "SLIDING_WINDOW capacity must be <= 100000");
        }
        ruleId = ruleId.trim();
        stateVersion = stateVersion.trim();
    }

    public RateLimitRequest(
            String ruleId,
            String stateVersion,
            String keyHash,
            long capacity,
            long refillTokens,
            Duration refillPeriod,
            long requestedTokens) {
        this(ruleId, stateVersion, keyHash,
                AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                capacity, refillTokens, refillPeriod, requestedTokens);
    }

    @Override
    public String toString() {
        return "RateLimitRequest[ruleId=" + ruleId + ", stateVersion=" + stateVersion
                + ", algorithm=" + algorithm
                + ", keyHash=<redacted>, capacity=" + capacity + ", refillTokens=" + refillTokens
                + ", refillPeriod=" + refillPeriod + ", requestedTokens=" + requestedTokens + "]";
    }
}
