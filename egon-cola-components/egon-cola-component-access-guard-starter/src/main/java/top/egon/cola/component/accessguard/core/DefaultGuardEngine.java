package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.core.failure.FailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailureResolution;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;
import top.egon.cola.component.accessguard.key.GuardKeyResolver;
import top.egon.cola.component.accessguard.execution.ExecutorRejectedException;
import top.egon.cola.component.accessguard.execution.RejectionHandler;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.RoutingTimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimitExceededException;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
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
    private final TimeLimiter timeLimiter;
    private final RejectionHandler rejectionHandler;
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
        this(
                planResolver,
                keyResolver,
                policies,
                localPolicies,
                failurePolicyResolver,
                penaltyService,
                Map.of(),
                (invocation, rejected, config) -> {
                    throw new AccessGuardRejectedException(rejected);
                },
                ticker,
                storage,
                engine);
    }

    public DefaultGuardEngine(
            GuardPlanResolver planResolver,
            GuardKeyResolver keyResolver,
            List<GuardPolicy<?>> policies,
            Map<String, GuardPolicy<?>> localPolicies,
            FailurePolicyResolver failurePolicyResolver,
            PenaltyService penaltyService,
            Map<TimeLimiterType, TimeLimiter> timeLimiters,
            RejectionHandler rejectionHandler,
            LongSupplier ticker,
            String storage,
            String engine
    ) {
        this(
                planResolver,
                keyResolver,
                policies,
                localPolicies,
                failurePolicyResolver,
                penaltyService,
                new RoutingTimeLimiter(timeLimiters),
                rejectionHandler,
                ticker,
                storage,
                engine);
    }

    public DefaultGuardEngine(
            GuardPlanResolver planResolver,
            GuardKeyResolver keyResolver,
            List<GuardPolicy<?>> policies,
            Map<String, GuardPolicy<?>> localPolicies,
            FailurePolicyResolver failurePolicyResolver,
            PenaltyService penaltyService,
            TimeLimiter timeLimiter,
            RejectionHandler rejectionHandler,
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
        this.timeLimiter = Objects.requireNonNull(timeLimiter, "timeLimiter");
        this.rejectionHandler = Objects.requireNonNull(rejectionHandler, "rejectionHandler");
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
        return evaluateAdmission(invocation, null);
    }

    private GuardOutcome evaluateAdmission(GuardInvocation invocation, PlanCapture capture) {
        Objects.requireNonNull(invocation, "invocation");
        long startedAt = ticker.getAsLong();
        if (capture != null) {
            capture.startedAt = startedAt;
        }
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
        if (capture != null) {
            capture.snapshot = snapshot;
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
                FailureResolution resolution = resolveStoreFailure(policy, state.context(), config, plan);
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
        Objects.requireNonNull(invocation, "invocation");
        PlanCapture capture = new PlanCapture();
        GuardOutcome admission = evaluateAdmission(invocation, capture);
        if (capture.snapshot == null) {
            throw new AccessGuardRejectedException(admission);
        }
        ExecutionConfig execution = capture.snapshot.plan().execution();
        if (admission.type() == GuardOutcomeType.REJECTED || admission.type() == GuardOutcomeType.FAILED) {
            return resolveRejection(invocation, admission, execution.rejection(), capture.startedAt);
        }
        try {
            Object value = executeOperation(invocation, execution.timeLimit());
            return new GuardExecutionResult<>(value, withElapsed(admission, capture.startedAt));
        } catch (TimeLimitExceededException exception) {
            GuardOutcome timedOut = executionFailure(
                    admission,
                    GuardDecision.TIME_LIMIT_EXCEEDED,
                    "TIME_LIMIT_EXCEEDED",
                    capture.startedAt);
            return resolveRejection(invocation, timedOut, execution.rejection(), capture.startedAt);
        } catch (ExecutorRejectedException exception) {
            GuardOutcome rejected = executionFailure(
                    admission,
                    GuardDecision.EXECUTOR_REJECTED,
                    "EXECUTOR_REJECTED",
                    capture.startedAt);
            return resolveRejection(invocation, rejected, execution.rejection(), capture.startedAt);
        } catch (AccessGuardRejectedException exception) {
            throw exception;
        } catch (Throwable throwable) {
            GuardOutcome failed = executionFailure(
                    admission,
                    GuardDecision.BUSINESS_EXCEPTION,
                    "BUSINESS_EXCEPTION",
                    capture.startedAt);
            return resolveRejection(invocation, failed, execution.rejection(), capture.startedAt);
        }
    }

    private Object executeOperation(
            GuardInvocation invocation,
            ExecutionConfig.TimeLimitConfig config
    ) throws Throwable {
        if (!config.enabled() || config.mode() == TimeLimitMode.DISABLED) {
            return invocation.continuation().execute();
        }
        return timeLimiter.execute(invocation, config);
    }

    private GuardExecutionResult<Object> resolveRejection(
            GuardInvocation invocation,
            GuardOutcome rejected,
            ExecutionConfig.RejectionConfig config,
            long startedAt
    ) throws Throwable {
        try {
            Object value = rejectionHandler.resolve(invocation, rejected, config);
            GuardOutcome resolved = new GuardOutcome(
                    GuardOutcomeType.DEGRADED,
                    rejected.decision(),
                    resolutionFor(config.mode()),
                    rejected.ruleId(),
                    rejected.policy(),
                    rejected.planVersion(),
                    rejected.storage(),
                    rejected.engine(),
                    elapsed(startedAt),
                    rejected.retryAfter(),
                    rejected.failure());
            return new GuardExecutionResult<>(value, resolved);
        } catch (AccessGuardRejectedException exception) {
            throw exception;
        } catch (Throwable throwable) {
            GuardOutcome failed = new GuardOutcome(
                    GuardOutcomeType.FAILED,
                    rejected.decision(),
                    GuardResolution.THROWN,
                    rejected.ruleId(),
                    rejected.policy(),
                    rejected.planVersion(),
                    rejected.storage(),
                    rejected.engine(),
                    elapsed(startedAt),
                    rejected.retryAfter(),
                    new GuardFailure("EXECUTION", "REJECTION_RESOLUTION_FAILED"));
            throw new AccessGuardRejectedException(failed);
        }
    }

    private GuardOutcome executionFailure(
            GuardOutcome admission,
            GuardDecision decision,
            String code,
            long startedAt
    ) {
        return new GuardOutcome(
                GuardOutcomeType.FAILED,
                decision,
                GuardResolution.THROWN,
                admission.ruleId(),
                "execution",
                admission.planVersion(),
                admission.storage(),
                admission.engine(),
                elapsed(startedAt),
                Duration.ZERO,
                new GuardFailure("EXECUTION", code));
    }

    private GuardOutcome withElapsed(GuardOutcome outcome, long startedAt) {
        return new GuardOutcome(
                outcome.type(),
                outcome.decision(),
                outcome.resolution(),
                outcome.ruleId(),
                outcome.policy(),
                outcome.planVersion(),
                outcome.storage(),
                outcome.engine(),
                elapsed(startedAt),
                outcome.retryAfter(),
                outcome.failure());
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
            GuardPlan plan
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

    private static GuardResolution resolutionFor(RejectionMode mode) {
        return switch (mode) {
            case THROW -> GuardResolution.THROWN;
            case FALLBACK -> GuardResolution.FALLBACK;
            case RETURN_JSON -> GuardResolution.RETURN_JSON;
            case RETURN_NULL -> GuardResolution.RETURN_NULL;
        };
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

    private static final class PlanCapture {

        private GuardPlanSnapshot snapshot;

        private long startedAt;
    }
}
