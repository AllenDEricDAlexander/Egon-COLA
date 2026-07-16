package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;

public record BridgeConstructorInvocation(
        Class<?> declaringClass,
        long methodId,
        Object[] arguments,
        BridgeFailHint failHint
) {
    public BridgeConstructorInvocation {
        Objects.requireNonNull(declaringClass, "declaringClass");
        Objects.requireNonNull(failHint, "failHint");
        arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
}
