package top.egon.cola.component.methodextension.context;

import java.lang.reflect.Method;
import java.util.Objects;

public final class MethodExtensionContext {

    private final Object target;

    private final Method method;

    private final Object[] arguments;

    public MethodExtensionContext(Object target, Method method, Object[] arguments) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public Object target() {
        return target;
    }

    public Method method() {
        return method;
    }

    public Object[] arguments() {
        return arguments.clone();
    }
}
