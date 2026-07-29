package top.egon.cola.component.accessguard.core;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.failure.DefaultFailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailurePolicyResolver;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.RejectionHandler;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimitExceededException;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyScope;
import top.egon.cola.component.accessguard.policy.AdmissionPolicies;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.accessguard.policy.allow.AllowListPolicy;
import top.egon.cola.component.accessguard.policy.deny.DenyListPolicy;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyBoxPolicy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitPolicy;
import top.egon.cola.component.accessguard.store.PenaltyStore;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGuardEngineTest {

    @Test
    void allowListNeverBypassesDenyList() throws Exception {
        AtomicInteger laterCalls = new AtomicInteger();
        DefaultGuardEngine engine = engine(
                (rule, version, hash) -> true,
                (rule, version, hash) -> {
                    laterCalls.incrementAndGet();
                    return true;
                },
                key -> {
                    laterCalls.incrementAndGet();
                    return Optional.empty();
                },
                request -> {
                    laterCalls.incrementAndGet();
                    return new RateLimitDecision(true, 1, Duration.ZERO);
                },
                FailurePolicies.defaults(),
                AllowListMode.BYPASS_RATE_LIMIT_AND_PENALTY,
                new DefaultFailurePolicyResolver());

        GuardOutcome outcome = engine.evaluate(invocation(new AtomicInteger()));

        assertThat(outcome.decision()).isEqualTo(GuardDecision.DENY_LIST_HIT);
        assertThat(laterCalls).hasValue(0);
    }

    @Test
    void allowListCanBypassPenaltyAndRateOnly() throws Exception {
        AtomicInteger laterCalls = new AtomicInteger();
        DefaultGuardEngine engine = engine(
                (rule, version, hash) -> false,
                (rule, version, hash) -> true,
                key -> {
                    laterCalls.incrementAndGet();
                    return Optional.empty();
                },
                request -> {
                    laterCalls.incrementAndGet();
                    return new RateLimitDecision(true, 1, Duration.ZERO);
                },
                FailurePolicies.defaults(),
                AllowListMode.BYPASS_RATE_LIMIT_AND_PENALTY,
                new DefaultFailurePolicyResolver());

        assertThat(engine.evaluate(invocation(new AtomicInteger())).type()).isEqualTo(GuardOutcomeType.ALLOWED);
        assertThat(laterCalls).hasValue(0);
    }

    @Test
    void failOpenIsDegradedNotPass() throws Exception {
        FailurePolicies policies = FailurePolicies.uniform(top.egon.cola.component.accessguard.core.failure.FailurePolicy.FAIL_OPEN);
        DefaultGuardEngine engine = engine(
                (rule, version, hash) -> {
                    throw new StoreOperationException("redis");
                },
                (rule, version, hash) -> true,
                key -> Optional.empty(),
                request -> new RateLimitDecision(true, 1, Duration.ZERO),
                policies,
                AllowListMode.GATE,
                new DefaultFailurePolicyResolver());

        GuardOutcome outcome = engine.evaluate(invocation(new AtomicInteger()));

        assertThat(outcome.type()).isEqualTo(GuardOutcomeType.DEGRADED);
        assertThat(outcome.decision()).isEqualTo(GuardDecision.STORE_FAILED);
        assertThat(outcome.resolution()).isEqualTo(GuardResolution.FAIL_OPEN);
    }

    @Test
    void realRateLimitIsTerminalAndNeverConsultsFailureResolver() throws Exception {
        FailurePolicyResolver forbidden = (point, policies, failure, fallback) -> {
            throw new AssertionError("real rejection must not resolve infrastructure failure");
        };
        DefaultGuardEngine engine = engine(
                (rule, version, hash) -> false,
                (rule, version, hash) -> false,
                key -> Optional.empty(),
                request -> new RateLimitDecision(false, 0, Duration.ofSeconds(1)),
                FailurePolicies.defaults(),
                AllowListMode.BYPASS_RATE_LIMIT,
                forbidden);

        GuardOutcome outcome = engine.evaluate(invocation(new AtomicInteger()));

        assertThat(outcome.decision()).isEqualTo(GuardDecision.RATE_LIMITED);
        assertThat(outcome.type()).isEqualTo(GuardOutcomeType.REJECTED);
    }

    @Test
    void executeNeverInvokesBusinessCodeAfterRejection() throws Exception {
        AtomicInteger businessCalls = new AtomicInteger();
        DefaultGuardEngine engine = engine(
                (rule, version, hash) -> true,
                (rule, version, hash) -> false,
                key -> Optional.empty(),
                request -> new RateLimitDecision(true, 1, Duration.ZERO),
                FailurePolicies.defaults(),
                AllowListMode.GATE,
                new DefaultFailurePolicyResolver());

        assertThatThrownBy(() -> engine.execute(invocation(businessCalls)))
                .isInstanceOf(AccessGuardRejectedException.class);
        assertThat(businessCalls).hasValue(0);
    }

    @Test
    void timeoutFallbackKeepsTimeoutDecision() throws Throwable {
        ExecutionConfig execution = new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(
                        true,
                        TimeLimitMode.ENFORCE,
                        TimeLimiterType.VIRTUAL_THREAD,
                        Duration.ofMillis(50),
                        true),
                new ExecutionConfig.RejectionConfig(RejectionMode.FALLBACK, "fallback", ""));
        GuardPlanSnapshot snapshot = snapshot(
                FailurePolicies.defaults(),
                AllowListMode.GATE,
                execution);
        TimeLimiter timedOut = (invocation, config) -> {
            throw new TimeLimitExceededException(config.timeout());
        };
        RejectionHandler fallback = (invocation, outcome, config) -> "fallback";
        DefaultGuardEngine engine = executionEngine(snapshot, Map.of(TimeLimiterType.VIRTUAL_THREAD, timedOut), fallback);
        AtomicInteger businessCalls = new AtomicInteger();

        GuardExecutionResult<Object> result = engine.executeWithOutcome(invocation(businessCalls));

        assertThat(result.value()).isEqualTo("fallback");
        assertThat(result.outcome().type()).isEqualTo(GuardOutcomeType.DEGRADED);
        assertThat(result.outcome().decision()).isEqualTo(GuardDecision.TIME_LIMIT_EXCEEDED);
        assertThat(result.outcome().resolution()).isEqualTo(GuardResolution.FALLBACK);
        assertThat(businessCalls).hasValue(0);
    }

    @Test
    void rejectionRendererFailureNeverRunsBusinessOperation() throws Exception {
        GuardPlanSnapshot snapshot = snapshot(FailurePolicies.defaults(), AllowListMode.GATE);
        RejectionHandler failedRenderer = (invocation, outcome, config) -> {
            throw new IllegalStateException("render failed");
        };
        DefaultGuardEngine engine = executionEngine(
                snapshot,
                Map.of(),
                failedRenderer,
                (rule, version, hash) -> true);
        AtomicInteger businessCalls = new AtomicInteger();

        assertThatThrownBy(() -> engine.execute(invocation(businessCalls)))
                .isInstanceOf(AccessGuardRejectedException.class);
        assertThat(businessCalls).hasValue(0);
    }

    private static DefaultGuardEngine engine(
            top.egon.cola.component.accessguard.store.DenyListStore denyStore,
            top.egon.cola.component.accessguard.store.AllowListStore allowStore,
            PenaltyStore penaltyStore,
            top.egon.cola.component.accessguard.store.RateLimitBackend rateBackend,
            FailurePolicies failures,
            AllowListMode allowMode,
            FailurePolicyResolver failureResolver
    ) {
        GuardPlanSnapshot snapshot = snapshot(failures, allowMode);
        DenyListPolicy deny = new DenyListPolicy(denyStore);
        AllowListPolicy allow = new AllowListPolicy(allowStore);
        PenaltyBoxPolicy penalty = new PenaltyBoxPolicy(penaltyStore);
        RateLimitPolicy rate = new RateLimitPolicy(rateBackend);
        return new DefaultGuardEngine(
                ruleId -> snapshot,
                (invocation, config) -> new GuardKeyResolution(GuardKeyScope.KEY, List.of(), hash()),
                AdmissionPolicies.builtIns(deny, allow, penalty, rate),
                Map.of("penalty-box", penalty, "rate-limit", rate),
                failureResolver,
                (context, config) -> new top.egon.cola.component.accessguard.store.PenaltyState(0, false, null, null),
                System::nanoTime,
                "LOCAL",
                "PROGRAMMATIC");
    }

    private static GuardPlanSnapshot snapshot(FailurePolicies failures, AllowListMode allowMode) {
        return snapshot(
                failures,
                allowMode,
                new ExecutionConfig(
                        new ExecutionConfig.TimeLimitConfig(false, TimeLimitMode.DISABLED,
                                TimeLimiterType.CALLER_THREAD, Duration.ofSeconds(1), true),
                        new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", "")));
    }

    private static GuardPlanSnapshot snapshot(
            FailurePolicies failures,
            AllowListMode allowMode,
            ExecutionConfig execution
    ) {
        GuardPlan plan = new GuardPlan(
                "draw",
                true,
                new KeyConfig(List.of("GLOBAL"), List.of(), "secret"),
                new AdmissionConfig(
                        new AdmissionConfig.DenyListConfig(true),
                        new AdmissionConfig.AllowListConfig(true, allowMode),
                        new AdmissionConfig.PenaltyBoxConfig(true, 3, Duration.ofMinutes(1), Duration.ofMinutes(10)),
                        new AdmissionConfig.RateLimitConfig(true, AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                                10, 10, Duration.ofSeconds(1), 1)),
                execution,
                failures,
                ObservabilityConfig.defaults(),
                "state-v1");
        return new GuardPlanSnapshot("draw", 1L, Instant.EPOCH, "test", plan, "fingerprint");
    }

    private static DefaultGuardEngine executionEngine(
            GuardPlanSnapshot snapshot,
            Map<TimeLimiterType, TimeLimiter> timeLimiters,
            RejectionHandler rejectionHandler
    ) {
        return executionEngine(snapshot, timeLimiters, rejectionHandler, (rule, version, hash) -> false);
    }

    private static DefaultGuardEngine executionEngine(
            GuardPlanSnapshot snapshot,
            Map<TimeLimiterType, TimeLimiter> timeLimiters,
            RejectionHandler rejectionHandler,
            top.egon.cola.component.accessguard.store.DenyListStore denyStore
    ) {
        DenyListPolicy deny = new DenyListPolicy(denyStore);
        AllowListPolicy allow = new AllowListPolicy((rule, version, hash) -> true);
        PenaltyBoxPolicy penalty = new PenaltyBoxPolicy(key -> Optional.empty());
        RateLimitPolicy rate = new RateLimitPolicy(
                request -> new RateLimitDecision(true, 1, Duration.ZERO));
        return new DefaultGuardEngine(
                ruleId -> snapshot,
                (invocation, config) -> new GuardKeyResolution(GuardKeyScope.KEY, List.of(), hash()),
                AdmissionPolicies.builtIns(deny, allow, penalty, rate),
                Map.of("penalty-box", penalty, "rate-limit", rate),
                new DefaultFailurePolicyResolver(),
                (context, config) -> new top.egon.cola.component.accessguard.store.PenaltyState(0, false, null, null),
                timeLimiters,
                rejectionHandler,
                System::nanoTime,
                "LOCAL",
                "PROGRAMMATIC");
    }

    private static GuardInvocation invocation(AtomicInteger calls) throws Exception {
        Method method = Sample.class.getDeclaredMethod("draw");
        return new GuardInvocation("draw", new Sample(), Sample.class, method, new Object[0], Map.of(),
                GuardEntryType.PROGRAMMATIC, GuardInvocationKind.METHOD,
                () -> {
                    calls.incrementAndGet();
                    return "ok";
                });
    }

    private static String hash() {
        return "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
    }

    static class Sample {
        String draw() {
            return "ok";
        }
    }
}
