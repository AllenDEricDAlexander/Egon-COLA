package top.egon.cola.component.bytecode.core.enhance;

import java.util.Set;

public record MethodEnhancementPlan(
        String methodName,
        String methodDescriptor,
        Set<EnhancementFeature> features,
        int candidateCount
) {
    public MethodEnhancementPlan {
        features = Set.copyOf(features);
    }
}
