package top.egon.cola.component.accessguard.core;

public interface GuardEngine {

    GuardOutcome evaluate(GuardInvocation invocation);

    default PreparedGuardExecution prepare(GuardInvocation invocation) {
        throw new UnsupportedOperationException("GuardEngine does not support deferred execution");
    }

    Object execute(GuardInvocation invocation) throws Throwable;
}
