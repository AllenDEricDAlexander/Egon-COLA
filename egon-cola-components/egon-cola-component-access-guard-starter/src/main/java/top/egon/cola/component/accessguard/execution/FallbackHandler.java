package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardOutcome;

public interface FallbackHandler {

    Object execute(GuardInvocation invocation, GuardOutcome outcome, String fallbackMethod) throws Throwable;
}
