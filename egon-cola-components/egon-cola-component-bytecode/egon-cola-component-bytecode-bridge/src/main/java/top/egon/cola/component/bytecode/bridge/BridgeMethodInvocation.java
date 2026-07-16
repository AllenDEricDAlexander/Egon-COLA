package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;

public record BridgeMethodInvocation(
        Object target,
        Class<?> declaringClass,
        long methodId,
        Object[] arguments
) {
    public BridgeMethodInvocation {
        Objects.requireNonNull(declaringClass, "declaringClass");
        arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
}
