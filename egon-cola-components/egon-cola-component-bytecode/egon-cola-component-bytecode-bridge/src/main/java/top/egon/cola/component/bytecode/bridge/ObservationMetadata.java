package top.egon.cola.component.bytecode.bridge;

import java.util.Map;
import java.util.Objects;

public record ObservationMetadata(
        long methodId,
        String layer,
        Map<String, String> staticTags,
        long slowThresholdNanos
) {
    public ObservationMetadata {
        Objects.requireNonNull(layer, "layer");
        staticTags = staticTags == null ? Map.of() : Map.copyOf(staticTags);
    }
}
