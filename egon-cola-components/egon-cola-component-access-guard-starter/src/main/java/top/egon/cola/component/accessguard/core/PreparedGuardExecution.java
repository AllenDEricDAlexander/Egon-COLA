package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.observability.GuardInvocationFinalizer;

import java.util.Objects;

public final class PreparedGuardExecution {

    private final GuardInvocation invocation;
    private final GuardOutcome admission;
    private final ExecutionConfig execution;
    private final RejectionResolver rejectionResolver;
    private final FailureFactory failureFactory;
    private final CompletionResolver completionResolver;
    private final GuardInvocationFinalizer finalizer;

    PreparedGuardExecution(
            GuardInvocation invocation,
            GuardOutcome admission,
            ExecutionConfig execution,
            RejectionResolver rejectionResolver,
            FailureFactory failureFactory,
            CompletionResolver completionResolver,
            GuardInvocationFinalizer finalizer
    ) {
        this.invocation = Objects.requireNonNull(invocation, "invocation");
        this.admission = Objects.requireNonNull(admission, "admission");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.rejectionResolver = Objects.requireNonNull(rejectionResolver, "rejectionResolver");
        this.failureFactory = Objects.requireNonNull(failureFactory, "failureFactory");
        this.completionResolver = Objects.requireNonNull(completionResolver, "completionResolver");
        this.finalizer = Objects.requireNonNull(finalizer, "finalizer");
    }

    public GuardInvocation invocation() {
        return invocation;
    }

    public GuardOutcome admission() {
        return admission;
    }

    public ExecutionConfig execution() {
        return execution;
    }

    public boolean admitted() {
        return admission.type() != GuardOutcomeType.REJECTED
                && admission.type() != GuardOutcomeType.FAILED;
    }

    public GuardExecutionResult<Object> resolveAdmission() throws Throwable {
        if (admitted()) {
            throw new IllegalStateException("admitted execution does not have an admission rejection");
        }
        return rejectionResolver.resolve(admission);
    }

    public GuardExecutionResult<Object> resolveFailure(GuardDecision decision, String code) throws Throwable {
        return rejectionResolver.resolve(failureFactory.create(decision, code));
    }

    public GuardExecutionResult<Object> complete(Object value) {
        return completionResolver.complete(value);
    }

    public GuardOutcome cancel() {
        GuardOutcome outcome = failureFactory.create(GuardDecision.CANCELLED, "CANCELLED");
        finalizer.finish(outcome);
        return outcome;
    }

    public boolean finish(GuardOutcome outcome) {
        return finalizer.finish(outcome);
    }

    public boolean finish(GuardExecutionResult<?> result) {
        return finish(Objects.requireNonNull(result, "result").outcome());
    }

    public GuardOutcome finishResolutionFailure(GuardOutcome original) {
        Objects.requireNonNull(original, "original");
        GuardOutcome failed = new GuardOutcome(
                GuardOutcomeType.FAILED,
                original.decision(),
                GuardResolution.THROWN,
                original.ruleId(),
                original.policy(),
                original.planVersion(),
                original.storage(),
                original.engine(),
                original.elapsed(),
                original.retryAfter(),
                new GuardFailure("EXECUTION", "REJECTION_RESOLUTION_FAILED"));
        finalizer.finish(failed);
        return failed;
    }

    public void stage(String stage, GuardOutcome outcome) {
        finalizer.stage(stage, outcome);
    }

    @FunctionalInterface
    interface RejectionResolver {

        GuardExecutionResult<Object> resolve(GuardOutcome rejected) throws Throwable;
    }

    @FunctionalInterface
    interface FailureFactory {

        GuardOutcome create(GuardDecision decision, String code);
    }

    @FunctionalInterface
    interface CompletionResolver {

        GuardExecutionResult<Object> complete(Object value);
    }
}
