package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.component.ddc.service.DdcConfigApplier;

import java.util.Objects;

/**
 * Adapts one exact DDC key to the atomic RBAC3 runtime policy.
 */
public final class Rbac3DdcPolicyApplier implements DdcConfigApplier {

    private final String key;
    private final int priority;
    private final AtomicRbac3RuntimePolicy policy;
    private final ApplyObserver observer;

    public Rbac3DdcPolicyApplier(
            String key,
            int priority,
            AtomicRbac3RuntimePolicy policy) {
        this(key, priority, policy, ApplyObserver.noop());
    }

    public Rbac3DdcPolicyApplier(
            String key,
            int priority,
            AtomicRbac3RuntimePolicy policy,
            ApplyObserver observer) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.key = key;
        this.priority = priority;
        this.policy = Objects.requireNonNull(policy, "policy");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    @Override
    public void apply(String actualKey, String value, long version) {
        if (!key.equals(actualKey)) {
            throw new IllegalArgumentException("unexpected RBAC3 config key: " + actualKey);
        }
        try {
            policy.apply(actualKey, value, version);
            observer.recordApply(key, "success");
        } catch (RuntimeException failure) {
            observer.recordApply(key, "failed");
            throw failure;
        }
    }

    @Override
    public int priority() {
        return priority;
    }

    @FunctionalInterface
    public interface ApplyObserver {

        void recordApply(String key, String status);

        static ApplyObserver noop() {
            return (key, status) -> {
            };
        }
    }
}
