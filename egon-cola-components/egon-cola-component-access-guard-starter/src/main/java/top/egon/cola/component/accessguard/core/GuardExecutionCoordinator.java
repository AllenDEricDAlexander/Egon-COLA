package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.execution.ExecutorRejectedException;
import top.egon.cola.component.accessguard.execution.RejectionHandler;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimitExceededException;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.observability.GuardEventPublisher;
import top.egon.cola.component.accessguard.observability.GuardInvocationFinalizer;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Builds and executes the shared prepared Guard lifecycle.
 */
public final class GuardExecutionCoordinator {

    private final TimeLimiter timeLimiter;
    private final RejectionHandler rejectionHandler;
    private final LongSupplier ticker;
    private final GuardEventPublisher eventPublisher;

    public GuardExecutionCoordinator(
            TimeLimiter timeLimiter,
            RejectionHandler rejectionHandler,
            LongSupplier ticker,
            GuardEventPublisher eventPublisher
    ) {
        this.timeLimiter = Objects.requireNonNull(timeLimiter, "timeLimiter");
        this.rejectionHandler = Objects.requireNonNull(rejectionHandler, "rejectionHandler");
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public PreparedGuardExecution prepare(
            GuardInvocation invocation,
            GuardAdmission admission
    ) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(admission, "admission");
        ExecutionConfig execution = admission.execution();
        PreparedGuardExecution.RejectionResolver rejectionResolver = rejected ->
                resolveRejection(invocation, rejected, execution.rejection(), admission.startedAtNanos());
        GuardInvocationFinalizer finalizer =
                new GuardInvocationFinalizer(eventPublisher, admission.observability());
        return new PreparedGuardExecution(
                invocation,
                admission.outcome(),
                execution,
                rejectionResolver,
                (decision, code) -> executionFailure(
                        admission.outcome(), decision, code, admission.startedAtNanos()),
                value -> new GuardExecutionResult<>(
                        value, withElapsed(admission.outcome(), admission.startedAtNanos())),
                finalizer);
    }

    public GuardExecutionResult<Object> execute(PreparedGuardExecution prepared) throws Throwable {
        Objects.requireNonNull(prepared, "prepared");
        prepared.stage("admission", prepared.admission());
        if (!prepared.admitted()) {
            return resolveAndFinish(prepared, prepared::resolveAdmission);
        }
        try {
            Object value = executeOperation(prepared.invocation(), prepared.execution().timeLimit());
            GuardExecutionResult<Object> result = prepared.complete(value);
            prepared.finish(result);
            return result;
        } catch (TimeLimitExceededException exception) {
            return resolveAndFinish(prepared, () -> prepared.resolveFailure(
                    GuardDecision.TIME_LIMIT_EXCEEDED, "TIME_LIMIT_EXCEEDED"));
        } catch (ExecutorRejectedException exception) {
            return resolveAndFinish(prepared, () -> prepared.resolveFailure(
                    GuardDecision.EXECUTOR_REJECTED, "EXECUTOR_REJECTED"));
        } catch (AccessGuardRejectedException exception) {
            prepared.finish(exception.outcome());
            throw exception;
        } catch (Throwable throwable) {
            return resolveAndFinish(prepared, () -> prepared.resolveFailure(
                    GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION"));
        }
    }

    private GuardExecutionResult<Object> resolveAndFinish(
            PreparedGuardExecution prepared,
            Resolution resolution
    ) throws Throwable {
        try {
            GuardExecutionResult<Object> result = resolution.resolve();
            prepared.finish(result);
            return result;
        } catch (AccessGuardRejectedException exception) {
            prepared.finish(exception.outcome());
            throw exception;
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

    @FunctionalInterface
    private interface Resolution {

        GuardExecutionResult<Object> resolve() throws Throwable;
    }
}
