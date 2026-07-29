package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record FailurePolicies(Map<FailurePoint, FailurePolicy> policies) {

    public FailurePolicies {
        EnumMap<FailurePoint, FailurePolicy> copy = new EnumMap<>(FailurePoint.class);
        if (policies != null) {
            copy.putAll(policies);
        }
        policies = Map.copyOf(copy);
    }

    public static FailurePolicies uniform(FailurePolicy policy) {
        Objects.requireNonNull(policy, "policy");
        EnumMap<FailurePoint, FailurePolicy> policies = new EnumMap<>(FailurePoint.class);
        for (FailurePoint point : FailurePoint.values()) {
            policies.put(point, policy);
        }
        return new FailurePolicies(policies);
    }

    public static FailurePolicies defaults() {
        EnumMap<FailurePoint, FailurePolicy> policies = new EnumMap<>(FailurePoint.class);
        policies.put(FailurePoint.KEY_RESOLUTION, FailurePolicy.FAIL_CLOSED);
        policies.put(FailurePoint.DENY_LIST_STORE, FailurePolicy.FAIL_CLOSED);
        policies.put(FailurePoint.ALLOW_LIST_STORE, FailurePolicy.FAIL_CLOSED);
        policies.put(FailurePoint.PENALTY_STORE, FailurePolicy.LOCAL_FALLBACK);
        policies.put(FailurePoint.RATE_LIMIT_BACKEND, FailurePolicy.LOCAL_FALLBACK);
        policies.put(FailurePoint.EXECUTION, FailurePolicy.FAIL_CLOSED);
        policies.put(FailurePoint.OBSERVABILITY, FailurePolicy.FAIL_OPEN);
        return new FailurePolicies(policies);
    }

    public FailurePolicy policyFor(FailurePoint point) {
        FailurePolicy policy = policies.get(point);
        if (policy == null) {
            throw new IllegalStateException("No failure policy configured for " + point);
        }
        return policy;
    }
}
