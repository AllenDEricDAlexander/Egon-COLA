package top.egon.cola.component.accessguard.execution.reactive;

import top.egon.cola.component.accessguard.core.GuardInvocation;

public interface ReactiveGuardExecutor {

    boolean supports(Class<?> returnType);

    Object guard(GuardInvocation invocation, Class<?> returnType);
}
