package top.egon.cola.component.accessguard.policy.ratelimit;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;

/** Storage-local implementation of one rate-limit algorithm. */
public interface RateLimitAlgorithmStrategy {

    AdmissionConfig.RateLimitAlgorithm algorithm();

    RateLimitDecision acquire(RateLimitRequest request);

    default int evictExpired() {
        return 0;
    }

    default int size() {
        return 0;
    }
}
