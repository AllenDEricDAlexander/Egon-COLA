package top.egon.cola.component.methodextension.event;

import java.util.Objects;

public record MethodExtensionEvent(
        String methodSignature,
        String handlerType,
        String outcome,
        String reason
) {
    public MethodExtensionEvent {
        methodSignature = bounded(methodSignature, 256);
        handlerType = bounded(handlerType, 192);
        outcome = bounded(outcome, 16);
        reason = bounded(reason, 256);
        Objects.requireNonNull(methodSignature, "methodSignature");
        Objects.requireNonNull(handlerType, "handlerType");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(reason, "reason");
    }

    private static String bounded(String value, int maximumLength) {
        String safe = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
        return safe.length() <= maximumLength ? safe : safe.substring(0, maximumLength);
    }
}
