package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.core.failure.FailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailureResolution;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;
import top.egon.cola.component.accessguard.key.GuardKeyResolver;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyService;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Resolves one immutable plan snapshot and evaluates the typed admission policies.
 */
public final class GuardAdmissionPipeline {

    private static final List<GuardPolicyType> BUILT_IN_POLICY_TYPES =
            GuardPolicyType.canonicalOrder();

    private final GuardPlanResolver planResolver;
    private final GuardKeyResolver keyResolver;
    private final List<GuardPolicy> policies;
    private final Map<GuardPolicyType, GuardPolicy> localPolicies;
    private final FailurePolicyResolver failurePolicyResolver;
    private final PenaltyService penaltyService;
    private final LongSupplier ticker;
    private final String storage;
    private final String engine;

    public GuardAdmissionPipeline(
            GuardPlanResolver planResolver,
            GuardKeyResolver keyResolver,
            List<GuardPolicy> policies,
            Map<GuardPolicyType, GuardPolicy> localPolicies,
            FailurePolicyResolver failurePolicyResolver,
            PenaltyService penaltyService,
            LongSupplier ticker,
            String storage,
            String engine
    ) {
        this.planResolver = Objects.requireNonNull(planResolver, "planResolver");
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
        this.policies = validatePolicies(policies);
        this.localPolicies = validateLocalPolicies(localPolicies);
        this.failurePolicyResolver = Objects.requireNonNull(failurePolicyResolver, "failurePolicyResolver");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService");
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.storage = requireText(storage, "storage");
        this.engine = requireText(engine, "engine");
    }

    public GuardAdmission evaluate(GuardInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        long startedAt = ticker.getAsLong();
        GuardPlanSnapshot snapshot;
        try {
            snapshot = Objects.requireNonNull(
                    planResolver.resolve(invocation.ruleId()), "resolved plan snapshot");
        } catch (RuntimeException exception) {
            GuardOutcome outcome = outcome(
                    GuardOutcomeType.FAILED,
                    GuardDecision.CONFIG_FAILED,
                    GuardResolution.THROWN,
                    invocation.ruleId(),
                    "",
                    0,
                    Duration.ZERO,
                    new GuardFailure("CONFIG", "PLAN_RESOLUTION_FAILED"),
                    startedAt);
            return new GuardAdmission(
                    outcome, unavailableExecution(), ObservabilityConfig.defaults(), startedAt);
        }

        GuardPlan plan = snapshot.plan();
        if (!plan.enabled()) {
            return admission(
                    outcome(
                            GuardOutcomeType.ALLOWED,
                            GuardDecision.PASS,
                            GuardResolution.NONE,
                            invocation.ruleId(),
                            "",
                            snapshot.version(),
                            Duration.ZERO,
                            null,
                            startedAt),
                    snapshot,
                    startedAt);
        }

        GuardKeyResolution keyResolution;
        try {
            keyResolution = keyResolver.resolve(invocation, plan.key());
        } catch (RuntimeException exception) {
            return admission(
                    resolveKeyFailure(invocation.ruleId(), snapshot, exception, startedAt),
                    snapshot,
                    startedAt);
        }

        GuardContext context = GuardContext.forPolicy(
                invocation.ruleId(),
                snapshot.version(),
                plan.stateVersion(),
                keyResolution.keyHash());
        GuardExecutionState state = GuardExecutionState.initial(snapshot, context);
        for (GuardPolicy policy : policies) {
            GuardPolicyType policyType = policy.type();
            if (state.bypassedPolicies().contains(policyType)) {
                continue;
            }
            PolicyResult result;
            try {
                result = policy.evaluate(state.context(), plan.admission());
            } catch (StoreOperationException exception) {
                FailureResolution resolution = resolveStoreFailure(policy, state.context(), plan);
                GuardOutcome terminal = terminalFailureOutcome(
                        invocation.ruleId(), snapshot, policyType, resolution, startedAt);
                if (terminal != null) {
                    return admission(terminal, snapshot, startedAt);
                }
                result = resolution.policy() == FailurePolicy.FAIL_OPEN
                        ? PolicyResult.pass()
                        : resolution.localResult();
                GuardResolution guardResolution = resolution.policy() == FailurePolicy.FAIL_OPEN
                        ? GuardResolution.FAIL_OPEN
                        : GuardResolution.LOCAL_FALLBACK;
                state = state.degraded(
                        GuardDecision.STORE_FAILED,
                        guardResolution,
                        policyType.id(),
                        resolution.failure());
            }
            if (!result.allowed()) {
                recordRateLimitViolation(policyType, result, state.context(), plan.admission().penaltyBox());
                return admission(
                        outcome(
                                GuardOutcomeType.REJECTED,
                                result.decision(),
                                GuardResolution.THROWN,
                                invocation.ruleId(),
                                policyType.id(),
                                snapshot.version(),
                                result.retryAfter(),
                                state.failure(),
                                startedAt),
                        snapshot,
                        startedAt);
            }
            state = state.withBypassedPolicies(result.bypassedPolicies());
        }
        if (state.isDegraded()) {
            return admission(
                    outcome(
                            GuardOutcomeType.DEGRADED,
                            state.degradedDecision(),
                            state.degradedResolution(),
                            invocation.ruleId(),
                            state.degradedPolicy(),
                            snapshot.version(),
                            Duration.ZERO,
                            state.failure(),
                            startedAt),
                    snapshot,
                    startedAt);
        }
        return admission(
                outcome(
                        GuardOutcomeType.ALLOWED,
                        GuardDecision.PASS,
                        GuardResolution.NONE,
                        invocation.ruleId(),
                        "",
                        snapshot.version(),
                        Duration.ZERO,
                        null,
                        startedAt),
                snapshot,
                startedAt);
    }

    private GuardAdmission admission(
            GuardOutcome outcome,
            GuardPlanSnapshot snapshot,
            long startedAt
    ) {
        return new GuardAdmission(
                outcome,
                snapshot.plan().execution(),
                snapshot.plan().observability(),
                startedAt);
    }

    private GuardOutcome resolveKeyFailure(
            String ruleId,
            GuardPlanSnapshot snapshot,
            RuntimeException exception,
            long startedAt
    ) {
        String code = exception instanceof GuardKeyResolutionException keyException
                ? keyException.code()
                : "KEY_RESOLUTION_FAILED";
        GuardFailure failure = new GuardFailure("KEY", code);
        FailureResolution resolution = failurePolicyResolver.resolve(
                FailurePoint.KEY_RESOLUTION,
                snapshot.plan().failurePolicies(),
                failure,
                null);
        if (resolution.policy() == FailurePolicy.FAIL_OPEN) {
            return outcome(
                    GuardOutcomeType.DEGRADED,
                    GuardDecision.KEY_RESOLUTION_FAILED,
                    GuardResolution.FAIL_OPEN,
                    ruleId,
                    "key",
                    snapshot.version(),
                    Duration.ZERO,
                    failure,
                    startedAt);
        }
        return outcome(
                GuardOutcomeType.FAILED,
                GuardDecision.KEY_RESOLUTION_FAILED,
                GuardResolution.THROWN,
                ruleId,
                "key",
                snapshot.version(),
                Duration.ZERO,
                resolution.failure(),
                startedAt);
    }

    private FailureResolution resolveStoreFailure(
            GuardPolicy policy,
            GuardContext context,
            GuardPlan plan
    ) {
        GuardFailure failure = new GuardFailure("STORE", "OPERATION_FAILED");
        Supplier<PolicyResult> localFallback = null;
        GuardPolicy localPolicy = localPolicies.get(policy.type());
        if (localPolicy != null) {
            localFallback = () -> localPolicy.evaluate(context, plan.admission());
        }
        return failurePolicyResolver.resolve(
                policy.type().failurePoint(),
                plan.failurePolicies(),
                failure,
                localFallback);
    }

    private GuardOutcome terminalFailureOutcome(
            String ruleId,
            GuardPlanSnapshot snapshot,
            GuardPolicyType policyType,
            FailureResolution resolution,
            long startedAt
    ) {
        if (resolution.policy() == FailurePolicy.FAIL_CLOSED) {
            return outcome(
                    GuardOutcomeType.FAILED,
                    GuardDecision.STORE_FAILED,
                    GuardResolution.THROWN,
                    ruleId,
                    policyType.id(),
                    snapshot.version(),
                    Duration.ZERO,
                    resolution.failure(),
                    startedAt);
        }
        if (resolution.policy() == FailurePolicy.LOCAL_FALLBACK && !resolution.localResult().allowed()) {
            return outcome(
                    GuardOutcomeType.REJECTED,
                    resolution.localResult().decision(),
                    GuardResolution.LOCAL_FALLBACK,
                    ruleId,
                    policyType.id(),
                    snapshot.version(),
                    resolution.localResult().retryAfter(),
                    resolution.failure(),
                    startedAt);
        }
        return null;
    }

    private void recordRateLimitViolation(
            GuardPolicyType policyType,
            PolicyResult result,
            GuardContext context,
            AdmissionConfig.PenaltyBoxConfig config
    ) {
        if (policyType != GuardPolicyType.RATE_LIMIT
                || result.decision() != GuardDecision.RATE_LIMITED || !config.enabled()) {
            return;
        }
        try {
            penaltyService.recordViolation(context, config);
        } catch (StoreOperationException ignored) {
            // The real rate-limit rejection remains terminal even if penalty recording is unavailable.
        }
    }

    private GuardOutcome outcome(
            GuardOutcomeType type,
            GuardDecision decision,
            GuardResolution resolution,
            String ruleId,
            String policy,
            long planVersion,
            Duration retryAfter,
            GuardFailure failure,
            long startedAt
    ) {
        return new GuardOutcome(
                type,
                decision,
                resolution,
                ruleId,
                policy,
                planVersion,
                storage,
                engine,
                elapsed(startedAt),
                retryAfter,
                failure);
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(Math.max(0, ticker.getAsLong() - startedAt));
    }

    private static ExecutionConfig unavailableExecution() {
        return new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(
                        false,
                        TimeLimitMode.DISABLED,
                        TimeLimiterType.CALLER_THREAD,
                        Duration.ofSeconds(1),
                        true),
                new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", ""));
    }

    private static List<GuardPolicy> validatePolicies(List<GuardPolicy> policies) {
        Objects.requireNonNull(policies, "policies");
        EnumMap<GuardPolicyType, GuardPolicy> indexed = new EnumMap<>(GuardPolicyType.class);
        for (GuardPolicy policy : policies) {
            GuardPolicy nonNullPolicy = Objects.requireNonNull(policy, "policy");
            GuardPolicyType type = Objects.requireNonNull(nonNullPolicy.type(), "policy.type");
            if (indexed.put(type, nonNullPolicy) != null) {
                throw new IllegalArgumentException("duplicate built-in policy type " + type);
            }
        }
        List<GuardPolicyType> types = policies.stream().map(GuardPolicy::type).toList();
        if (!types.equals(BUILT_IN_POLICY_TYPES)
                || !indexed.keySet().equals(Set.copyOf(BUILT_IN_POLICY_TYPES))) {
            throw new IllegalArgumentException(
                    "built-in policies must use the fixed order " + BUILT_IN_POLICY_TYPES);
        }
        return List.copyOf(policies);
    }

    private static Map<GuardPolicyType, GuardPolicy> validateLocalPolicies(
            Map<GuardPolicyType, GuardPolicy> localPolicies
    ) {
        Objects.requireNonNull(localPolicies, "localPolicies");
        EnumMap<GuardPolicyType, GuardPolicy> indexed = new EnumMap<>(GuardPolicyType.class);
        for (Map.Entry<GuardPolicyType, GuardPolicy> entry : localPolicies.entrySet()) {
            GuardPolicyType key = Objects.requireNonNull(entry.getKey(), "local policy type");
            GuardPolicy policy = Objects.requireNonNull(entry.getValue(), "local policy");
            if (policy.type() != key) {
                throw new IllegalArgumentException(
                        "local policy key does not match policy type: " + key);
            }
            if (indexed.put(key, policy) != null) {
                throw new IllegalArgumentException("duplicate local policy type " + key);
            }
        }
        Set<GuardPolicyType> expected = Set.of(GuardPolicyType.PENALTY_BOX, GuardPolicyType.RATE_LIMIT);
        if (!indexed.keySet().equals(expected)) {
            throw new IllegalArgumentException("local policies must provide exactly " + expected);
        }
        return Map.copyOf(indexed);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
