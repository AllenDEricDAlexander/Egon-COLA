package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.api.GuardedOperation;

import java.lang.reflect.Executable;
import java.util.Map;
import java.util.Objects;

public record GuardInvocation(
        String ruleId,
        Object target,
        Class<?> targetClass,
        Executable executable,
        Object[] arguments,
        Map<String, Object> attributes,
        GuardEntryType entryType,
        GuardInvocationKind kind,
        GuardedOperation<?> continuation
) {

    public GuardInvocation {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        ruleId = ruleId.trim();
        targetClass = Objects.requireNonNull(targetClass, "targetClass");
        arguments = arguments == null ? new Object[0] : arguments.clone();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        entryType = Objects.requireNonNull(entryType, "entryType");
        kind = Objects.requireNonNull(kind, "kind");
        continuation = Objects.requireNonNull(continuation, "continuation");
        if (kind != GuardInvocationKind.OPERATION && executable == null) {
            throw new IllegalArgumentException("executable is required for methods and constructors");
        }
    }

    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
}
