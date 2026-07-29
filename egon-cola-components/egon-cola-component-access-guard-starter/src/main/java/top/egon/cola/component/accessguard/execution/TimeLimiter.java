package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

public interface TimeLimiter {

    Object execute(GuardInvocation invocation, ExecutionConfig.TimeLimitConfig config) throws Throwable;
}
