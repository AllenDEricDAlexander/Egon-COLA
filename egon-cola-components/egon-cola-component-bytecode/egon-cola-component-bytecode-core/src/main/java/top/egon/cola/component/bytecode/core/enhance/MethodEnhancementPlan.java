package top.egon.cola.component.bytecode.core.enhance;

import java.util.Set;
import java.util.stream.Collectors;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.core.enhance.methodextension.MethodExtensionPolicy;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationPolicy;

public record MethodEnhancementPlan(
        String methodName,
        String methodDescriptor,
        Set<EnhancementFeature> features,
        int candidateCount,
        ObservationPolicy observationPolicy,
        MethodExtensionPolicy methodExtensionPolicy
) {
    public MethodEnhancementPlan {
        features = Set.copyOf(features);
    }

    public MethodEnhancementPlan(
            String methodName,
            String methodDescriptor,
            Set<EnhancementFeature> features,
            int candidateCount
    ) {
        this(methodName, methodDescriptor, features, candidateCount, null, null);
    }

    public Set<BridgeCapability> bridgeCapabilities() {
        return features.stream()
                .filter(feature -> feature != EnhancementFeature.EXECUTOR)
                .map(feature -> BridgeCapability.valueOf(feature.name()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
