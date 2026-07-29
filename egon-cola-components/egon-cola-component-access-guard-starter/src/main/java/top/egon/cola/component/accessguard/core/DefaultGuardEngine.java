package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.core.failure.FailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailureResolution;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;
import top.egon.cola.component.accessguard.key.GuardKeyResolver;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.PolicyConfig;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyService;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class DefaultGuardEngine implements GuardEngine {

    private static final List<String> BUILT_IN_POLICY_IDS =
            List.of("deny-list", "allow-list", "penalty-box", "rate-limit");

    private final GuardPlanResolver planResolver;
    private final GuardKeyResolver keyResolver;
    private final List<GuardPolicy<?>> policies;
    private final Map<String, GuardPolicy<?>> localPolicies;
    private final FailurePolicyResolver failurePolicyResolver;
    private final PenaltyService penaltyService;
    private final LongSupplier ticker;
    private final String storage;
    private final String engine;

    public DefaultGuardEngine(
            GuardPlanResolver planResolver,
            GuardKeyResolver keyResolver,
            List<GuardPolicy<?>> policies,
            Map<String, GuardPolicy<?>> localPolicies,
            FailurePolicyResolver failurePolicyResolver,
            PenaltyService penaltyService,
            LongSupplier ticker,
            String storage,
            String engine
    ) {
        this.planResolver = Objects.requireNonNull(planResolver, "planResolver");
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
        this.policies = List.copyOf(Objects.requireNonNull(policies, "policies"));
        this.localPolicies = Map.copyOf(Objects.requireNonNull(localPolicies, "localPolicies"));
        this.failurePolicyResolver = Objects.requireNonNull(failurePolicyResolver, "failurePolicyResolver");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService");
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.storage = requireText(storage, "storage");
        this.engine = requireText(engine, "engine");
        List<String> policyIds = this.policies.stream().map(GuardPolicy::id).toList();
        if (!policyIds.equals(BUILT_IN_POLICY_IDS)) {
            throw new IllegalArgumentException("built-in policies must use the fixed order " + BUILT_IN_POLICY_IDS);
        }
    }

    @Override
    public GuardOutcome evaluate(GuardInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        long startedAt = ticker.getAsLong();
        GuardPlanSnapshot snapshot;
        try {
            snapshot = planResolver.resolve(invocation.ruleId());
        } catch (RuntimeException exception) {
            return outcome(
                    GuardOutcomeType.FAILED,
                    GuardDecision.CONFIG_FAILED,
                    GuardResolution.THROWN,
                    invocation.ruleId(),
                    "",
                    0,
                    Duration.ZERO,
                    new GuardFailure("CONFIG", "PLAN_RESOLUTION_FAILED"),
                    startedAt);
        }
        GuardPlan plan = snapshot.plan();
        if (!plan.enabled()) {
            return outcome(
                    GuardOutcomeType.ALLOWED,
                    GuardDecision.PASS,
                    GuardResolution.NONE,
                    invocation.ruleId(),
                    "",
                    snapshot.version(),
                    Duration.ZERO,
                    null,
                    startedAt);
        }

        GuardKeyResolution keyResolution;
        try {
            keyResolution = keyResolver.resolve(invocation, plan.key());
        } catch (RuntimeException exception) {
            return resolveKeyFailure(invocation.ruleId(), snapshot, exception, startedAt);
        }

        GuardContext context = GuardContext.forPolicy(
                invocation.ruleId(),
                snapshot.version(),
                plan.stateVersion(),
                keyResolution.keyHash());
        GuardExecutionState state = GuardExecutionState.initial(snapshot, context);
        for (GuardPolicy<?> policy : policies) {
            if (state.bypassedPolicies().contains(policy.id())) {
                continue;
            }
            PolicyConfig config = configFor(plan.admission(), policy.id());
            PolicyResult result;
            try {
                result = evaluatePolicy(policy, state.context(), config);
            } catch (StoreOperationException exception) {
                FailureResolution resolution = resolveStoreFailure(policy, state.context(), config, plan, exception);
                GuardOutcome terminal = terminalFailureOutcome(
                        invocation.ruleId(), snapshot, policy.id(), resolution, startedAt);
                if (terminal != null) {
                    return terminal;
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
                        policy.id(),
                        resolution.failure());
            }
            if (!result.allowed()) {
                recordRateLimitViolation(policy.id(), result, state.context(), plan.admission().penaltyBox());
                return outcome(
                        GuardOutcomeType.REJECTED,
                        result.decision(),
                        GuardResolution.THROWN,
                        invocation.ruleId(),
                        policy.id(),
                        snapshot.version(),
                        result.retryAfter(),
                        state.failure(),
                        startedAt);
            }
            state = state.withBypassedPolicies(result.bypassedPolicies());
        }
        if (state.isDegraded()) {
            return outcome(
                    GuardOutcomeType.DEGRADED,
                    state.degradedDecision(),
                    state.degradedResolution(),
                    invocation.ruleId(),
                    state.degradedPolicy(),
                    snapshot.version(),
                    Duration.ZERO,
                    state.failure(),
                    startedAt);
        }
        return outcome(
                GuardOutcomeType.ALLOWED,
                GuardDecision.PASS,
                GuardResolution.NONE,
                invocation.ruleId(),
                "",
                snapshot.version(),
                Duration.ZERO,
                null,
                startedAt);
    }

    @Override
    public Object execute(GuardInvocation invocation) throws Throwable {
        return executeWithOutcome(invocation).value();
    }

    GuardExecutionResult<Object> executeWithOutcome(GuardInvocation invocation) throws Throwable {
        GuardOutcome outcome = evaluate(invocation);
        if (outcome.type() == GuardOutcomeType.REJECTED || outcome.type() == GuardOutcomeType.FAILED) {
            throw new AccessGuardRejectedException(outcome);
        }
        return new GuardExecutionResult<>(invocation.continuation().execute(), outcome);
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
            GuardPolicy<?> policy,
            GuardContext context,
            PolicyConfig config,
            GuardPlan plan,
            StoreOperationException exception
    ) {
        GuardFailure failure = new GuardFailure("STORE", "OPERATION_FAILED");
        Supplier<PolicyResult> localFallback = null;
        GuardPolicy<?> localPolicy = localPolicies.get(policy.id());
        if (localPolicy != null) {
            localFallback = () -> evaluatePolicy(localPolicy, context, config);
        }
        return failurePolicyResolver.resolve(
                failurePoint(policy.id()),
                plan.failurePolicies(),
                failure,
                localFallback);
    }

    private GuardOutcome terminalFailureOutcome(
            String ruleId,
            GuardPlanSnapshot snapshot,
            String policyId,
            FailureResolution resolution,
            long startedAt
    ) {
        if (resolution.policy() == FailurePolicy.FAIL_CLOSED) {
            return outcome(
                    GuardOutcomeType.FAILED,
                    GuardDecision.STORE_FAILED,
                    GuardResolution.THROWN,
                    ruleId,
                    policyId,
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
                    policyId,
                    snapshot.version(),
                    resolution.localResult().retryAfter(),
                    resolution.failure(),
                    startedAt);
        }
        return null;
    }

    private void recordRateLimitViolation(
            String policyId,
            PolicyResult result,
            GuardContext context,
            AdmissionConfig.PenaltyBoxConfig config
    ) {
        if (!"rate-limit".equals(policyId) || result.decision() != GuardDecision.RATE_LIMITED || !config.enabled()) {
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
        long elapsedNanos = Math.max(0, ticker.getAsLong() - startedAt);
        return new GuardOutcome(
                type,
                decision,
                resolution,
                ruleId,
                policy,
                planVersion,
                storage,
                engine,
                Duration.ofNanos(elapsedNanos),
                retryAfter,
                failure);
    }

    private static FailurePoint failurePoint(String policyId) {
        return switch (policyId) {
            case "deny-list" -> FailurePoint.DENY_LIST_STORE;
            case "allow-list" -> FailurePoint.ALLOW_LIST_STORE;
            case "penalty-box" -> FailurePoint.PENALTY_STORE;
            case "rate-limit" -> FailurePoint.RATE_LIMIT_BACKEND;
            default -> throw new IllegalArgumentException("Unknown built-in policy " + policyId);
        };
    }

    private static PolicyConfig configFor(AdmissionConfig admission, String policyId) {
        return switch (policyId) {
            case "deny-list" -> admission.denyList();
            case "allow-list" -> admission.allowList();
            case "penalty-box" -> admission.penaltyBox();
            case "rate-limit" -> admission.rateLimit();
            default -> throw new IllegalArgumentException("Unknown built-in policy " + policyId);
        };
    }

    @SuppressWarnings("unchecked")
    private static <C extends PolicyConfig> PolicyResult evaluatePolicy(
            GuardPolicy<?> policy,
            GuardContext context,
            PolicyConfig config
    ) {
        return ((GuardPolicy<C>) policy).evaluate(context, (C) config);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
