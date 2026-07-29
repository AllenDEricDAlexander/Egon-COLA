package top.egon.cola.component.accessguard.core;

public interface GuardEngine {

    GuardOutcome evaluate(GuardInvocation invocation);

    Object execute(GuardInvocation invocation) throws Throwable;
}
