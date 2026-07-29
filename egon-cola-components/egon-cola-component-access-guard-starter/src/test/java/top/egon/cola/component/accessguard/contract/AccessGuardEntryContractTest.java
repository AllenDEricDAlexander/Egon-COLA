package top.egon.cola.component.accessguard.contract;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.framework.ProxyFactory;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.adapter.aop.SpringAopAccessGuardAdvisor;
import top.egon.cola.component.accessguard.adapter.programmatic.DefaultAccessGuardClient;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.api.GuardRequest;
import top.egon.cola.component.accessguard.core.DefaultGuardEngine;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.GuardResolution;
import top.egon.cola.component.accessguard.core.failure.DefaultFailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitExceededException;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyScope;
import top.egon.cola.component.accessguard.observability.CompositeGuardEventPublisher;
import top.egon.cola.component.accessguard.observability.GuardEvent;
import top.egon.cola.component.accessguard.policy.AdmissionPolicies;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.accessguard.policy.allow.AllowListPolicy;
import top.egon.cola.component.accessguard.policy.deny.DenyListPolicy;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyBoxPolicy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitPolicy;
import top.egon.cola.component.accessguard.store.PenaltyState;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardEntryContractTest {

    private static final String RULE_ID = "contract";
    private static final String KEY_HASH =
            "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void aopAndProgrammaticEntriesHaveIdenticalOutcomes(Scenario scenario) throws Throwable {
        EntryResult expected = runProgrammatic(scenario);
        EntryResult actual = runAop(scenario);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        assertThat(actual.businessCalls()).isEqualTo(expected.businessCalls());
        assertThat(actual.finalEvents()).containsExactlyElementsOf(expected.finalEvents());
        assertExpectedScenario(scenario, actual);
    }

    private static EntryResult runProgrammatic(Scenario scenario) {
        List<GuardEvent> events = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        DefaultAccessGuardClient client = new DefaultAccessGuardClient(engine(scenario, events));
        GuardRequest request = new GuardRequest(
                RULE_ID, new Object[0], Map.of(), String.class, null);
        return invoke(calls, events, () -> client.execute(request, () -> business(calls)));
    }

    private static EntryResult runAop(Scenario scenario) {
        List<GuardEvent> events = new ArrayList<>();
        ContractTarget target = new ContractTarget();
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvisor(new SpringAopAccessGuardAdvisor(
                new GuardBindingResolver(), engine(scenario, events)));
        ContractTarget proxy = (ContractTarget) factory.getProxy();
        return invoke(target.calls, events, proxy::draw);
    }

    private static EntryResult invoke(
            AtomicInteger calls,
            List<GuardEvent> events,
            GuardedCall call
    ) {
        Object value = null;
        String failureType = "";
        try {
            value = call.execute();
        } catch (Throwable failure) {
            failureType = failure.getClass().getName();
        }
        return new EntryResult(
                value,
                failureType,
                calls.get(),
                events.stream().map(GuardEvent::outcome).toList());
    }

    private static DefaultGuardEngine engine(Scenario scenario, List<GuardEvent> events) {
        DenyListPolicy deny = new DenyListPolicy((ruleId, dataVersion, keyHash) -> {
            if (scenario == Scenario.FAIL_OPEN || scenario == Scenario.LOCAL_FALLBACK) {
                throw new StoreOperationException("primary unavailable");
            }
            return scenario == Scenario.DENY || scenario == Scenario.FALLBACK;
        });
        AllowListPolicy allow = new AllowListPolicy((ruleId, dataVersion, keyHash) -> false);
        PenaltyBoxPolicy penalty = new PenaltyBoxPolicy(key -> scenario == Scenario.PENALTY
                ? Optional.of(new PenaltyState(
                3, true, Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(600)))
                : Optional.empty());
        RateLimitPolicy rate = new RateLimitPolicy(request -> scenario == Scenario.RATE_LIMIT
                ? new RateLimitDecision(false, 0, Duration.ofSeconds(1))
                : new RateLimitDecision(true, 1, Duration.ZERO));
        DenyListPolicy localDeny = new DenyListPolicy((ruleId, dataVersion, keyHash) -> false);
        TimeLimiter timeLimiter = (invocation, config) -> {
            if (scenario == Scenario.TIMEOUT) {
                throw new TimeLimitExceededException(config.timeout());
            }
            return invocation.continuation().execute();
        };
        return new DefaultGuardEngine(
                ruleId -> snapshot(scenario),
                (invocation, config) -> new GuardKeyResolution(
                        GuardKeyScope.GLOBAL, List.of(), KEY_HASH),
                AdmissionPolicies.builtIns(deny, allow, penalty, rate),
                Map.of("deny-list", localDeny, "penalty-box", penalty, "rate-limit", rate),
                new DefaultFailurePolicyResolver(),
                (context, config) -> new PenaltyState(0, false, null, null),
                timeLimiter,
                (invocation, outcome, config) -> {
                    if (config.mode() == RejectionMode.FALLBACK) {
                        return "fallback";
                    }
                    throw new AccessGuardRejectedException(outcome);
                },
                () -> 0L,
                "LOCAL",
                "CONTRACT",
                new CompositeGuardEventPublisher(List.of(events::add)));
    }

    private static GuardPlanSnapshot snapshot(Scenario scenario) {
        boolean denyEnabled = switch (scenario) {
            case DENY, FAIL_OPEN, LOCAL_FALLBACK, FALLBACK -> true;
            default -> false;
        };
        AdmissionConfig admission = new AdmissionConfig(
                new AdmissionConfig.DenyListConfig(denyEnabled),
                new AdmissionConfig.AllowListConfig(false, AllowListMode.GATE),
                new AdmissionConfig.PenaltyBoxConfig(
                        scenario == Scenario.PENALTY,
                        3,
                        Duration.ofMinutes(1),
                        Duration.ofMinutes(10)),
                new AdmissionConfig.RateLimitConfig(
                        scenario == Scenario.RATE_LIMIT,
                        AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                        10,
                        10,
                        Duration.ofSeconds(1),
                        1));
        ExecutionConfig execution = new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(
                        scenario == Scenario.TIMEOUT,
                        scenario == Scenario.TIMEOUT ? TimeLimitMode.ENFORCE : TimeLimitMode.DISABLED,
                        scenario == Scenario.TIMEOUT
                                ? TimeLimiterType.VIRTUAL_THREAD : TimeLimiterType.CALLER_THREAD,
                        Duration.ofMillis(50),
                        true),
                new ExecutionConfig.RejectionConfig(
                        scenario == Scenario.FALLBACK ? RejectionMode.FALLBACK : RejectionMode.THROW,
                        scenario == Scenario.FALLBACK ? "fallback" : "",
                        ""));
        GuardPlan plan = new GuardPlan(
                RULE_ID,
                true,
                new KeyConfig(List.of("GLOBAL"), List.of(), "secret"),
                admission,
                execution,
                failurePolicies(scenario),
                ObservabilityConfig.defaults(),
                "state-v1");
        return new GuardPlanSnapshot(
                RULE_ID, 1L, Instant.EPOCH, "contract", plan, "fingerprint");
    }

    private static FailurePolicies failurePolicies(Scenario scenario) {
        EnumMap<FailurePoint, FailurePolicy> policies = new EnumMap<>(FailurePoint.class);
        policies.putAll(FailurePolicies.defaults().policies());
        if (scenario == Scenario.FAIL_OPEN) {
            policies.put(FailurePoint.DENY_LIST_STORE, FailurePolicy.FAIL_OPEN);
        } else if (scenario == Scenario.LOCAL_FALLBACK) {
            policies.put(FailurePoint.DENY_LIST_STORE, FailurePolicy.LOCAL_FALLBACK);
        }
        return new FailurePolicies(policies);
    }

    private static void assertExpectedScenario(Scenario scenario, EntryResult result) {
        GuardOutcome outcome = result.finalEvents().getFirst();
        switch (scenario) {
            case ALLOW -> assertThat(outcome.decision()).isEqualTo(GuardDecision.PASS);
            case DENY, FALLBACK -> assertThat(outcome.decision()).isEqualTo(GuardDecision.DENY_LIST_HIT);
            case PENALTY -> assertThat(outcome.decision()).isEqualTo(GuardDecision.PENALTY_ACTIVE);
            case RATE_LIMIT -> assertThat(outcome.decision()).isEqualTo(GuardDecision.RATE_LIMITED);
            case FAIL_OPEN -> assertThat(outcome.resolution()).isEqualTo(GuardResolution.FAIL_OPEN);
            case LOCAL_FALLBACK -> assertThat(outcome.resolution()).isEqualTo(GuardResolution.LOCAL_FALLBACK);
            case TIMEOUT -> assertThat(outcome.decision()).isEqualTo(GuardDecision.TIME_LIMIT_EXCEEDED);
        }
        assertThat(result.finalEvents()).hasSize(1);
        if (scenario == Scenario.FALLBACK) {
            assertThat(result.value()).isEqualTo("fallback");
            assertThat(result.businessCalls()).isZero();
        }
    }

    private static String business(AtomicInteger calls) {
        calls.incrementAndGet();
        return "business";
    }

    enum Scenario {
        ALLOW,
        DENY,
        PENALTY,
        RATE_LIMIT,
        FAIL_OPEN,
        LOCAL_FALLBACK,
        TIMEOUT,
        FALLBACK
    }

    record EntryResult(
            Object value,
            String failureType,
            int businessCalls,
            List<GuardOutcome> finalEvents
    ) {
    }

    @FunctionalInterface
    interface GuardedCall {

        Object execute() throws Throwable;
    }

    static class ContractTarget {

        private final AtomicInteger calls = new AtomicInteger();

        @AccessGuard(RULE_ID)
        public String draw() {
            return business(calls);
        }
    }
}
