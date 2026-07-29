package top.egon.cola.component.accessguard.core.failure;

import top.egon.cola.component.accessguard.core.GuardFailure;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.policy.PolicyResult;

import java.util.function.Supplier;

public interface FailurePolicyResolver {

    FailureResolution resolve(
            FailurePoint point,
            FailurePolicies policies,
            GuardFailure failure,
            Supplier<PolicyResult> localFallback);
}
