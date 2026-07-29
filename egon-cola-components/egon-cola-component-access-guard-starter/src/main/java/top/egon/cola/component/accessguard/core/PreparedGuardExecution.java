package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.util.Objects;

public final class PreparedGuardExecution {

    private final GuardInvocation invocation;
    private final GuardOutcome admission;
    private final ExecutionConfig execution;
    private final RejectionResolver rejectionResolver;
    private final FailureFactory failureFactory;
    private final CompletionResolver completionResolver;

    PreparedGuardExecution(
            GuardInvocation invocation,
            GuardOutcome admission,
            ExecutionConfig execution,
            RejectionResolver rejectionResolver,
            FailureFactory failureFactory,
            CompletionResolver completionResolver
    ) {
        this.invocation = Objects.requireNonNull(invocation, "invocation");
        this.admission = Objects.requireNonNull(admission, "admission");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.rejectionResolver = Objects.requireNonNull(rejectionResolver, "rejectionResolver");
        this.failureFactory = Objects.requireNonNull(failureFactory, "failureFactory");
        this.completionResolver = Objects.requireNonNull(completionResolver, "completionResolver");
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
        return failureFactory.create(GuardDecision.CANCELLED, "CANCELLED");
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
