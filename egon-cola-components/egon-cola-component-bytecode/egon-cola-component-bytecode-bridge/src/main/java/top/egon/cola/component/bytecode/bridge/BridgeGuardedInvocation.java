package top.egon.cola.component.bytecode.bridge;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record BridgeGuardedInvocation(
        Object target,
        Class<?> declaringClass,
        long methodId,
        Object[] arguments,
        MethodHandle continuation
) {
    public BridgeGuardedInvocation {
        Objects.requireNonNull(declaringClass, "declaringClass");
        Objects.requireNonNull(continuation, "continuation");
        arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public Object proceed() throws Throwable {
        List<Object> invocationArguments = new ArrayList<>();
        if (target != null) {
            invocationArguments.add(target);
        }
        Collections.addAll(invocationArguments, arguments);
        return continuation.invokeWithArguments(invocationArguments);
    }

    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
}
