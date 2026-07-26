package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectedException;

public class AccessGuardFailureHandler {

    private final AccessGuardProperties properties;

    public AccessGuardFailureHandler(AccessGuardProperties properties) {
        this.properties = properties;
    }

    /**
     * Decides whether a stage that could not reach a decision should let the call continue.
     *
     * <p>The switch is deliberately exhaustive over {@link FailStrategy}: adding a constant must
     * break the build here rather than silently fall through to rethrowing the infrastructure error.
     */
    public boolean failOpen(AccessGuardRule rule, String stage, RuntimeException failure) {
        return switch (effectiveStrategy(rule)) {
            case FAIL_OPEN, GLOBAL_DEFAULT -> true;
            case LOCAL_FALLBACK -> localFallback(stage, failure);
            case FAIL_CLOSED -> throw new AccessGuardRejectedException(
                    "Access Guard " + stage + " infrastructure failed", failure);
        };
    }

    /**
     * Degrades to the local decision path. Stages backed by a shared store carry their own local
     * implementation (see {@code RedissonRateLimiterExecutor}); the remaining stages continue with
     * their permissive local default.
     */
    private boolean localFallback(String stage, RuntimeException failure) {
        if (properties.getLocalFallback().isEnabled()) {
            return true;
        }
        throw new AccessGuardRejectedException(
                "Access Guard " + stage + " infrastructure failed and local fallback is disabled", failure);
    }

    public FailStrategy effectiveStrategy(AccessGuardRule rule) {
        FailStrategy strategy = rule == null ? FailStrategy.GLOBAL_DEFAULT : rule.failStrategy();
        if (strategy == null || strategy == FailStrategy.GLOBAL_DEFAULT) {
            strategy = properties.getFailStrategy();
        }
        return strategy == null || strategy == FailStrategy.GLOBAL_DEFAULT
                ? FailStrategy.FAIL_OPEN : strategy;
    }
}
