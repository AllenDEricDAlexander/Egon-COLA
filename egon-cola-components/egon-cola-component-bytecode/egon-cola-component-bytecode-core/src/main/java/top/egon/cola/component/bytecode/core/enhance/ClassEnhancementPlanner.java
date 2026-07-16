package top.egon.cola.component.bytecode.core.enhance;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.core.enhance.executor.ExecutorCallSiteEnhancer;
import top.egon.cola.component.bytecode.core.enhance.accessguard.AccessGuardMatcher;
import top.egon.cola.component.bytecode.core.enhance.accessguard.AccessGuardPolicy;
import top.egon.cola.component.bytecode.core.enhance.methodextension.MethodExtensionMatcher;
import top.egon.cola.component.bytecode.core.enhance.methodextension.MethodExtensionPolicy;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationMatcher;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationPolicy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ClassEnhancementPlanner {

    public ClassEnhancementPlan plan(ClassNode classNode) {
        return plan(null, classNode, null, null, null);
    }

    public ClassEnhancementPlan plan(
            ClassNode classNode,
            ObservationMatcher observationMatcher
    ) {
        return plan(null, classNode, observationMatcher, null, null);
    }

    public ClassEnhancementPlan plan(
            ClassLoader loader,
            ClassNode classNode,
            ObservationMatcher observationMatcher,
            MethodExtensionMatcher methodExtensionMatcher
    ) {
        return plan(loader, classNode, observationMatcher, methodExtensionMatcher, null);
    }

    public ClassEnhancementPlan plan(
            ClassLoader loader,
            ClassNode classNode,
            ObservationMatcher observationMatcher,
            MethodExtensionMatcher methodExtensionMatcher,
            AccessGuardMatcher accessGuardMatcher
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
            Optional<MethodExtensionPolicy> methodExtension = methodExtensionMatcher == null
                    ? Optional.empty()
                    : methodExtensionMatcher.match(loader, classNode, method);
            Optional<AccessGuardPolicy> accessGuard = accessGuardMatcher == null
                    ? Optional.empty() : accessGuardMatcher.match(classNode, method);
            EnumSet<EnhancementFeature> methodFeatures =
                    EnumSet.noneOf(EnhancementFeature.class);
            if (candidates > 0) {
                methodFeatures.add(EnhancementFeature.EXECUTOR);
            }
            if (observation.isPresent()) {
                methodFeatures.add(EnhancementFeature.OBSERVATION);
            }
            if (methodExtension.isPresent()) {
                methodFeatures.add(EnhancementFeature.METHOD_EXTENSION);
            }
            if (accessGuard.isPresent()) {
                methodFeatures.add(EnhancementFeature.ACCESS_GUARD);
            }
            if (!methodFeatures.isEmpty()) {
                methods.add(new MethodEnhancementPlan(
                        method.name,
                        method.desc,
                        methodFeatures,
                        candidates,
                        observation.orElse(null),
                        methodExtension.orElse(null),
                        accessGuard.orElse(null)
                ));
                classFeatures.addAll(methodFeatures);
            }
        }
        return new ClassEnhancementPlan(classNode.name, classFeatures, methods);
    }
}
