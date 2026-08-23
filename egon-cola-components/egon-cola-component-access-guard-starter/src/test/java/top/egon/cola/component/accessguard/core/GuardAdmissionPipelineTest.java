package top.egon.cola.component.accessguard.core;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.failure.DefaultFailurePolicyResolver;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyScope;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.PenaltyState;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardAdmissionPipelineTest {

    private static final String KEY_HASH =
            "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    @Test
    void resolvesOneSnapshotAndUsesCanonicalPolicies() throws Exception {
        AtomicInteger snapshotCalls = new AtomicInteger();
        AtomicInteger businessCalls = new AtomicInteger();
        List<GuardPolicyType> calls = new ArrayList<>();
        ExecutionConfig execution = executionConfig();
        ObservabilityConfig observability = new ObservabilityConfig(true, true, false, true, false);
        GuardPlanSnapshot snapshot = snapshot(execution, observability);
        List<GuardPolicy> policies = policies(calls);
        GuardAdmissionPipeline pipeline = new GuardAdmissionPipeline(
                ruleId -> {
                    snapshotCalls.incrementAndGet();
                    return snapshot;
                },
                (invocation, config) -> new GuardKeyResolution(GuardKeyScope.KEY, List.of(), KEY_HASH),
                policies,
                Map.of(
                        GuardPolicyType.PENALTY_BOX, policy(GuardPolicyType.PENALTY_BOX, new ArrayList<>()),
                        GuardPolicyType.RATE_LIMIT, policy(GuardPolicyType.RATE_LIMIT, new ArrayList<>())),
                new DefaultFailurePolicyResolver(),
                (context, config) -> new PenaltyState(0, false, null, null),
                () -> 100L,
                "REDIS",
                "AOP");

        GuardAdmission admission = pipeline.evaluate(invocation(businessCalls));

        assertThat(snapshotCalls).hasValue(1);
        assertThat(calls).containsExactlyElementsOf(GuardPolicyType.canonicalOrder());
        assertThat(admission.outcome().type()).isEqualTo(GuardOutcomeType.ALLOWED);
        assertThat(admission.outcome().planVersion()).isEqualTo(7L);
        assertThat(admission.outcome().storage()).isEqualTo("REDIS");
        assertThat(admission.outcome().engine()).isEqualTo("AOP");
        assertThat(admission.execution()).isSameAs(execution);
        assertThat(admission.observability()).isSameAs(observability);
        assertThat(admission.startedAtNanos()).isEqualTo(100L);
        assertThat(businessCalls).hasValue(0);
    }

    @Test
    void planResolutionFailureUsesSafeExecutionAndObservability() throws Exception {
        AtomicInteger businessCalls = new AtomicInteger();
        GuardAdmissionPipeline pipeline = new GuardAdmissionPipeline(
                ruleId -> {
                    throw new IllegalStateException("plan unavailable");
                },
                (invocation, config) -> new GuardKeyResolution(GuardKeyScope.KEY, List.of(), KEY_HASH),
                policies(new ArrayList<>()),
                Map.of(
                        GuardPolicyType.PENALTY_BOX, policy(GuardPolicyType.PENALTY_BOX, new ArrayList<>()),
                        GuardPolicyType.RATE_LIMIT, policy(GuardPolicyType.RATE_LIMIT, new ArrayList<>())),
                new DefaultFailurePolicyResolver(),
                (context, config) -> new PenaltyState(0, false, null, null),
                () -> 111L,
                "LOCAL",
                "PROGRAMMATIC");

        GuardAdmission admission = pipeline.evaluate(invocation(businessCalls));

        assertThat(admission.outcome().decision()).isEqualTo(GuardDecision.CONFIG_FAILED);
        assertThat(admission.outcome().type()).isEqualTo(GuardOutcomeType.FAILED);
        assertThat(admission.execution().timeLimit().mode()).isEqualTo(TimeLimitMode.DISABLED);
        assertThat(admission.execution().rejection().mode()).isEqualTo(RejectionMode.THROW);
        assertThat(admission.observability()).isEqualTo(ObservabilityConfig.defaults());
        assertThat(admission.startedAtNanos()).isEqualTo(111L);
        assertThat(businessCalls).hasValue(0);
    }

    @Test
    void rejectsDuplicateOrMissingBuiltInPolicies() {
        List<GuardPolicy> duplicate = List.of(
                policy(GuardPolicyType.DENY_LIST, new ArrayList<>()),
                policy(GuardPolicyType.DENY_LIST, new ArrayList<>()),
                policy(GuardPolicyType.PENALTY_BOX, new ArrayList<>()),
                policy(GuardPolicyType.RATE_LIMIT, new ArrayList<>()));

        assertThatThrownBy(() -> pipeline(duplicate, localPolicies()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
        assertThatThrownBy(() -> pipeline(policies(new ArrayList<>()).subList(0, 3), localPolicies()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixed order");
    }

    @Test
    void rejectsLocalPolicyKeyTypeMismatch() {
        Map<GuardPolicyType, GuardPolicy> mismatched = Map.of(
                GuardPolicyType.PENALTY_BOX, policy(GuardPolicyType.PENALTY_BOX, new ArrayList<>()),
                GuardPolicyType.RATE_LIMIT, policy(GuardPolicyType.PENALTY_BOX, new ArrayList<>()));

        assertThatThrownBy(() -> pipeline(policies(new ArrayList<>()), mismatched))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
    }

    private static List<GuardPolicy> policies(List<GuardPolicyType> calls) {
        return List.of(
                policy(GuardPolicyType.DENY_LIST, calls),
                policy(GuardPolicyType.ALLOW_LIST, calls),
                policy(GuardPolicyType.PENALTY_BOX, calls),
                policy(GuardPolicyType.RATE_LIMIT, calls));
    }

    private static GuardPolicy policy(GuardPolicyType type, List<GuardPolicyType> calls) {
        return new GuardPolicy() {
            @Override
            public GuardPolicyType type() {
                return type;
            }

            @Override
            public PolicyResult evaluate(GuardContext context, AdmissionConfig admission) {
                calls.add(type);
                return PolicyResult.pass();
            }
        };
    }

    private static Map<GuardPolicyType, GuardPolicy> localPolicies() {
        return Map.of(
                GuardPolicyType.PENALTY_BOX, policy(GuardPolicyType.PENALTY_BOX, new ArrayList<>()),
                GuardPolicyType.RATE_LIMIT, policy(GuardPolicyType.RATE_LIMIT, new ArrayList<>()));
    }

    private static GuardAdmissionPipeline pipeline(
            List<GuardPolicy> policies,
            Map<GuardPolicyType, GuardPolicy> localPolicies
    ) {
        return new GuardAdmissionPipeline(
                ruleId -> snapshot(executionConfig(), ObservabilityConfig.defaults()),
                (invocation, config) -> new GuardKeyResolution(GuardKeyScope.KEY, List.of(), KEY_HASH),
                policies,
                localPolicies,
                new DefaultFailurePolicyResolver(),
                (context, config) -> new PenaltyState(0, false, null, null),
                () -> 100L,
                "LOCAL",
                "AOP");
    }

    private static GuardPlanSnapshot snapshot(
            ExecutionConfig execution,
            ObservabilityConfig observability
    ) {
        AdmissionConfig admission = new AdmissionConfig(
                new AdmissionConfig.DenyListConfig(true),
                new AdmissionConfig.AllowListConfig(true,
                        top.egon.cola.component.accessguard.policy.allow.AllowListMode.GATE),
                new AdmissionConfig.PenaltyBoxConfig(true, 3,
                        Duration.ofMinutes(1), Duration.ofMinutes(10)),
                new AdmissionConfig.RateLimitConfig(true,
                        AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                        10, 10, Duration.ofSeconds(1), 1));
        GuardPlan plan = new GuardPlan(
                "draw",
                true,
                new KeyConfig(List.of("GLOBAL"), List.of(), "secret"),
                admission,
                execution,
                FailurePolicies.defaults(),
                observability,
                "state-v1");
        return new GuardPlanSnapshot("draw", 7L, Instant.EPOCH, "test", plan, "fingerprint");
    }

    private static ExecutionConfig executionConfig() {
        return new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(
                        false,
                        TimeLimitMode.DISABLED,
                        TimeLimiterType.CALLER_THREAD,
                        Duration.ofMillis(25),
                        true),
                new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", ""));
    }

    private static GuardInvocation invocation(AtomicInteger businessCalls) throws Exception {
        Method method = Sample.class.getDeclaredMethod("draw");
        return new GuardInvocation(
                "draw",
                new Sample(),
                Sample.class,
                method,
                new Object[0],
                Map.of(),
                GuardEntryType.PROGRAMMATIC,
                GuardInvocationKind.METHOD,
                () -> {
                    businessCalls.incrementAndGet();
                    return "business";
                });
    }

    static class Sample {

        String draw() {
            return "business";
        }
    }
}
