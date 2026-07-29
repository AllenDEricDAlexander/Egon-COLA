package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.util.Objects;

public final class CallerThreadTimeLimiter implements TimeLimiter {

    @Override
    public Object execute(GuardInvocation invocation, ExecutionConfig.TimeLimitConfig config) throws Throwable {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(config, "config");
        if (config.mode() != TimeLimitMode.OBSERVE_ONLY || config.executor() != TimeLimiterType.CALLER_THREAD) {
            throw new IllegalArgumentException("CALLER_THREAD is valid only for OBSERVE_ONLY");
        }
        return invocation.continuation().execute();
    }
}
