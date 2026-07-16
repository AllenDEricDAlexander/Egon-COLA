package top.egon.cola.component.bytecode.core.enhance;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.core.enhance.executor.ExecutorCallSiteEnhancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClassEnhancementPlanner {

    public ClassEnhancementPlan plan(ClassNode classNode) {
        List<MethodEnhancementPlan> methods = new ArrayList<>();
        for (MethodNode method : classNode.methods) {
            int candidates = 0;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode invocation
                        && ExecutorCallSiteEnhancer.supports(invocation)) {
                    candidates++;
                }
            }
            if (candidates > 0) {
                methods.add(new MethodEnhancementPlan(
                        method.name,
                        method.desc,
                        Set.of(EnhancementFeature.EXECUTOR),
                        candidates
                ));
            }
        }
        Set<EnhancementFeature> features = methods.isEmpty()
                ? Set.of() : Set.of(EnhancementFeature.EXECUTOR);
        return new ClassEnhancementPlan(classNode.name, features, methods);
    }
}
