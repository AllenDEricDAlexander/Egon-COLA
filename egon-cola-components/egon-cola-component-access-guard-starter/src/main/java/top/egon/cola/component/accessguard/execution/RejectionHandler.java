package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

public interface RejectionHandler {

    Object resolve(
            GuardInvocation invocation,
            GuardOutcome rejected,
            ExecutionConfig.RejectionConfig config) throws Throwable;
}
