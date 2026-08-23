package top.egon.cola.component.accessguard.core;

public interface GuardEngine {

    /**
     * Evaluates admission only; the default engine finishes the resulting outcome without running the continuation.
     */
    GuardOutcome evaluate(GuardInvocation invocation);

    /**
     * Creates the deferred lifecycle consumed by asynchronous and reactive adapters.
     */
    default PreparedGuardExecution prepare(GuardInvocation invocation) {
        throw new UnsupportedOperationException("GuardEngine does not support deferred execution");
    }

    /**
     * Executes one invocation and returns the business or rejection-resolution value.
     */
    Object execute(GuardInvocation invocation) throws Throwable;
}
