package top.egon.cola.component.bytecode.core.enhance;

import java.util.List;
import java.util.Set;

public record ClassEnhancementPlan(
        String className,
        Set<EnhancementFeature> features,
        List<MethodEnhancementPlan> methods
) {
    public ClassEnhancementPlan {
        features = Set.copyOf(features);
        methods = List.copyOf(methods);
    }

    public boolean empty() {
        return methods.isEmpty();
    }
}
