package top.egon.cola.component.gateway.engine.traffic;

import java.time.Duration;
import java.util.Objects;

public record LocalTokenBucketPolicy(
        String policyId,
        long stateEpoch,
        long capacity,
        long refillTokens,
        Duration refillPeriod,
        long initialTokens,
        int maximumKeys,
        Duration idleTtl
) {

    public LocalTokenBucketPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (stateEpoch < 0
                || capacity < 1
                || refillTokens < 1
                || initialTokens < 0
                || initialTokens > capacity
                || maximumKeys < 1) {
            throw new IllegalArgumentException(
                    "invalid local token bucket bounds"
            );
        }
        refillPeriod = positive(refillPeriod, "refillPeriod");
        idleTtl = positive(idleTtl, "idleTtl");
    }

    String stateKey(String keyHash) {
        return policyId + ":" + stateEpoch + ":" + keyHash;
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
