package top.egon.cola.component.accessguard.core.failure;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardFailure;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.policy.PolicyResult;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFailurePolicyResolverTest {

    private final DefaultFailurePolicyResolver resolver = new DefaultFailurePolicyResolver();
    private final GuardFailure failure = new GuardFailure("STORE", "UNAVAILABLE");

    @Test
    void exposesTheApprovedDefaultMatrix() {
        FailurePolicies policies = FailurePolicies.defaults();

        assertThat(policies.policyFor(FailurePoint.KEY_RESOLUTION)).isEqualTo(FailurePolicy.FAIL_CLOSED);
        assertThat(policies.policyFor(FailurePoint.DENY_LIST_STORE)).isEqualTo(FailurePolicy.FAIL_CLOSED);
        assertThat(policies.policyFor(FailurePoint.ALLOW_LIST_STORE)).isEqualTo(FailurePolicy.FAIL_CLOSED);
        assertThat(policies.policyFor(FailurePoint.PENALTY_STORE)).isEqualTo(FailurePolicy.LOCAL_FALLBACK);
        assertThat(policies.policyFor(FailurePoint.RATE_LIMIT_BACKEND)).isEqualTo(FailurePolicy.LOCAL_FALLBACK);
        assertThat(policies.policyFor(FailurePoint.OBSERVABILITY)).isEqualTo(FailurePolicy.FAIL_OPEN);
    }

    @Test
    void failOpenIsAnExplicitDegradedResolution() {
        FailurePolicies policies = FailurePolicies.uniform(FailurePolicy.FAIL_OPEN);

        assertThat(resolver.resolve(FailurePoint.DENY_LIST_STORE, policies, failure, null))
                .isEqualTo(FailureResolution.failOpen(failure));
    }

    @Test
    void successfulLocalFallbackKeepsThePrimaryFailure() {
        FailureResolution resolution = resolver.resolve(
                FailurePoint.RATE_LIMIT_BACKEND,
                FailurePolicies.defaults(),
                failure,
                PolicyResult::pass);

        assertThat(resolution.policy()).isEqualTo(FailurePolicy.LOCAL_FALLBACK);
        assertThat(resolution.localResult().decision()).isEqualTo(GuardDecision.PASS);
        assertThat(resolution.failure()).isEqualTo(failure);
    }

    @Test
    void failedLocalFallbackBecomesFailClosed() {
        FailureResolution resolution = resolver.resolve(
                FailurePoint.PENALTY_STORE,
                FailurePolicies.defaults(),
                failure,
                () -> {
                    throw new IllegalStateException("local down");
                });

        assertThat(resolution.policy()).isEqualTo(FailurePolicy.FAIL_CLOSED);
        assertThat(resolution.failure().code()).isEqualTo("LOCAL_FALLBACK_FAILED");
    }
}
