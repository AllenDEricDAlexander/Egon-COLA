package top.egon.cola.component.accessguard.policy.ratelimit;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

/** Validates and dispatches the complete set of configured rate-limit algorithms. */
public final class RateLimitAlgorithmStrategyFactory {

    private final EnumMap<AdmissionConfig.RateLimitAlgorithm, RateLimitAlgorithmStrategy> strategies;

    public RateLimitAlgorithmStrategyFactory(
            Iterable<RateLimitAlgorithmStrategy> configuredStrategies) {
        EnumMap<AdmissionConfig.RateLimitAlgorithm, RateLimitAlgorithmStrategy> mappings =
                new EnumMap<>(AdmissionConfig.RateLimitAlgorithm.class);
        if (configuredStrategies == null) {
            throw new IllegalArgumentException("rate-limit strategy coverage is incomplete");
        }
        for (RateLimitAlgorithmStrategy strategy : configuredStrategies) {
            RateLimitAlgorithmStrategy value = Objects.requireNonNull(
                    strategy, "rate-limit strategy");
            AdmissionConfig.RateLimitAlgorithm algorithm = Objects.requireNonNull(
                    value.algorithm(), "rate-limit algorithm");
            if (mappings.putIfAbsent(algorithm, value) != null) {
                throw new IllegalArgumentException(
                        "duplicate rate-limit strategy for " + algorithm);
            }
        }
        if (!mappings.keySet().equals(
                EnumSet.allOf(AdmissionConfig.RateLimitAlgorithm.class))) {
            throw new IllegalArgumentException(
                    "rate-limit strategy coverage is incomplete: "
                            + mappings.keySet());
        }
        this.strategies = new EnumMap<>(mappings);
    }

    public RateLimitDecision acquire(RateLimitRequest request) {
        RateLimitRequest value = Objects.requireNonNull(request, "request");
        return strategy(value.algorithm()).acquire(value);
    }

    public int evictExpired() {
        return strategies.values().stream()
                .mapToInt(RateLimitAlgorithmStrategy::evictExpired)
                .sum();
    }

    public int size() {
        return strategies.values().stream()
                .mapToInt(RateLimitAlgorithmStrategy::size)
                .sum();
    }

    private RateLimitAlgorithmStrategy strategy(
            AdmissionConfig.RateLimitAlgorithm algorithm) {
        return Objects.requireNonNull(
                strategies.get(algorithm),
                "rate-limit strategy is not configured: " + algorithm);
    }
}
