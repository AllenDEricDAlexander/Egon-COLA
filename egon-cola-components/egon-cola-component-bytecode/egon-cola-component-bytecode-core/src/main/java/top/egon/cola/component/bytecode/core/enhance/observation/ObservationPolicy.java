package top.egon.cola.component.bytecode.core.enhance.observation;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationMetadata;

import java.util.Map;
import java.util.Set;

public record ObservationPolicy(
        long methodId,
        String owner,
        String methodName,
        String methodDescriptor,
        int access,
        boolean constructor,
        String layer,
        Map<String, String> staticTags,
        long slowThresholdNanos
) {
    public ObservationPolicy {
        staticTags = Map.copyOf(staticTags);
    }

    public MethodMetadata methodMetadata() {
        return new MethodMetadata(
                methodId, owner, methodName, methodDescriptor, access, constructor,
                Set.of(BridgeCapability.OBSERVATION));
    }

    public ObservationMetadata observationMetadata() {
        return new ObservationMetadata(methodId, layer, staticTags, slowThresholdNanos);
    }
}
