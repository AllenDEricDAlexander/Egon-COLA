package top.egon.cola.component.bytecode.core.enhance;

import java.util.Set;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationPolicy;

public record MethodEnhancementPlan(
        String methodName,
        String methodDescriptor,
        Set<EnhancementFeature> features,
        int candidateCount,
        ObservationPolicy observationPolicy
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
        this(methodName, methodDescriptor, features, candidateCount, null);
    }
}
