package top.egon.cola.component.bytecode.core.enhance;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.core.enhance.executor.ExecutorCallSiteEnhancer;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationMatcher;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationPolicy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ClassEnhancementPlanner {

    public ClassEnhancementPlan plan(ClassNode classNode) {
        return plan(classNode, null);
    }

    public ClassEnhancementPlan plan(
            ClassNode classNode,
            ObservationMatcher observationMatcher
    ) {
        List<MethodEnhancementPlan> methods = new ArrayList<>();
        EnumSet<EnhancementFeature> classFeatures = EnumSet.noneOf(EnhancementFeature.class);
        for (MethodNode method : classNode.methods) {
            int candidates = 0;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode invocation
                        && ExecutorCallSiteEnhancer.supports(invocation)) {
                    candidates++;
                }
            }
            Optional<ObservationPolicy> observation = observationMatcher == null
                    ? Optional.empty() : observationMatcher.match(classNode.name, method);
            EnumSet<EnhancementFeature> methodFeatures =
                    EnumSet.noneOf(EnhancementFeature.class);
            if (candidates > 0) {
                methodFeatures.add(EnhancementFeature.EXECUTOR);
            }
            if (observation.isPresent()) {
                methodFeatures.add(EnhancementFeature.OBSERVATION);
            }
            if (!methodFeatures.isEmpty()) {
                methods.add(new MethodEnhancementPlan(
                        method.name,
                        method.desc,
                        methodFeatures,
                        candidates,
                        observation.orElse(null)
                ));
                classFeatures.addAll(methodFeatures);
            }
        }
        return new ClassEnhancementPlan(classNode.name, classFeatures, methods);
    }
}
