package top.egon.cola.component.accessguard.store;

@FunctionalInterface
public interface RateLimitBackend {

    RateLimitDecision acquire(RateLimitRequest request);

    default int evictExpired() {
        return 0;
    }

    default int size() {
        return 0;
    }
}
