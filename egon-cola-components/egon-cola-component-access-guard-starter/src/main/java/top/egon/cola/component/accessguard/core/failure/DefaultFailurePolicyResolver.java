package top.egon.cola.component.accessguard.core.failure;

import top.egon.cola.component.accessguard.core.GuardFailure;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.policy.PolicyResult;

import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultFailurePolicyResolver implements FailurePolicyResolver {

    @Override
    public FailureResolution resolve(
            FailurePoint point,
            FailurePolicies policies,
            GuardFailure failure,
            Supplier<PolicyResult> localFallback
    ) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(policies, "policies");
        Objects.requireNonNull(failure, "failure");
        FailurePolicy policy = policies.policyFor(point);
        if (policy == FailurePolicy.FAIL_OPEN) {
            return FailureResolution.failOpen(failure);
        }
        if (policy == FailurePolicy.FAIL_CLOSED) {
            return FailureResolution.failClosed(failure);
        }
        try {
            if (localFallback == null) {
                throw new IllegalStateException("local fallback is unavailable");
            }
            return FailureResolution.localFallback(
                    Objects.requireNonNull(localFallback.get(), "local fallback result"),
                    failure);
        } catch (RuntimeException exception) {
            return FailureResolution.failClosed(new GuardFailure("STORE", "LOCAL_FALLBACK_FAILED"));
        }
    }
}
