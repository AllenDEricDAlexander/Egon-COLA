package top.egon.cola.component.bytecode.api.observation;

import java.util.Map;
import java.util.Objects;

public record ObservationEvent(
        long methodId,
        String className,
        String methodName,
        String methodDescriptor,
        String layer,
        long durationNanos,
        ObservationResult result,
        String exceptionGroup,
        String traceId,
        boolean virtualThread,
        Map<String, String> staticTags,
        long slowThresholdNanos
) {
    public ObservationEvent {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(exceptionGroup, "exceptionGroup");
        traceId = traceId == null ? "" : traceId;
        staticTags = staticTags == null ? Map.of() : Map.copyOf(staticTags);
        if (durationNanos < 0L) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
    }
}
