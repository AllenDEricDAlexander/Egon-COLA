package top.egon.cola.component.accessguard.api;

import java.util.Map;
import java.util.Objects;

public record GuardRequest(
        String ruleId,
        Object[] arguments,
        Map<String, Object> attributes,
        Class<?> returnType,
        GuardedOperation<?> fallback
) {

    public GuardRequest {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        ruleId = ruleId.trim();
        arguments = arguments == null ? new Object[0] : arguments.clone();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        returnType = Objects.requireNonNull(returnType, "returnType");
    }

    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
}
