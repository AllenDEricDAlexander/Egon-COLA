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

    public FailurePolicy policyFor(FailurePoint point) {
        FailurePolicy policy = policies.get(point);
        if (policy == null) {
            throw new IllegalStateException("No failure policy configured for " + point);
        }
        return policy;
    }
}
