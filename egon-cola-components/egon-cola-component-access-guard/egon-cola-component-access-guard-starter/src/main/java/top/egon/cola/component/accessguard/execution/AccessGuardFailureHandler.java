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

    public boolean failOpen(AccessGuardRule rule, String stage, RuntimeException failure) {
        FailStrategy strategy = effectiveStrategy(rule);
        if (strategy == FailStrategy.FAIL_OPEN) {
            return true;
        }
        if (strategy == FailStrategy.FAIL_CLOSED) {
            throw new AccessGuardRejectedException(
                    "Access Guard " + stage + " infrastructure failed", failure);
        }
        throw failure;
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
