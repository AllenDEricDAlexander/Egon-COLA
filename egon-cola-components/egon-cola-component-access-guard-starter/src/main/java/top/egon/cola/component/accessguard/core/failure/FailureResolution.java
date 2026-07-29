package top.egon.cola.component.accessguard.core.failure;

import top.egon.cola.component.accessguard.core.GuardFailure;
import top.egon.cola.component.accessguard.policy.PolicyResult;

import java.util.Objects;

public record FailureResolution(
        FailurePolicy policy,
        PolicyResult localResult,
        GuardFailure failure
) {

    public FailureResolution {
        policy = Objects.requireNonNull(policy, "policy");
        failure = Objects.requireNonNull(failure, "failure");
        if (policy == FailurePolicy.LOCAL_FALLBACK && localResult == null) {
            throw new IllegalArgumentException("localResult is required for LOCAL_FALLBACK");
        }
    }

    public static FailureResolution failOpen(GuardFailure failure) {
        return new FailureResolution(FailurePolicy.FAIL_OPEN, null, failure);
    }

    public static FailureResolution failClosed(GuardFailure failure) {
        return new FailureResolution(FailurePolicy.FAIL_CLOSED, null, failure);
    }

    public static FailureResolution localFallback(PolicyResult result, GuardFailure failure) {
        return new FailureResolution(FailurePolicy.LOCAL_FALLBACK, result, failure);
    }
}
