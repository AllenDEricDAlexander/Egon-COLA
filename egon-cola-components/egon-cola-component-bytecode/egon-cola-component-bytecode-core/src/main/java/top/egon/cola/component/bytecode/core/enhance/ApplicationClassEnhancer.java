package top.egon.cola.component.bytecode.core.enhance;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import top.egon.cola.component.bytecode.core.enhance.executor.ExecutorCallSiteEnhancer;
import top.egon.cola.component.bytecode.core.enhance.methodextension.MethodExtensionEnhancer;
import top.egon.cola.component.bytecode.core.enhance.methodextension.MethodExtensionMatcher;
import top.egon.cola.component.bytecode.core.enhance.observation.MethodObservationEnhancer;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationMatcher;
import top.egon.cola.component.bytecode.core.hierarchy.EgonClassWriter;

public final class ApplicationClassEnhancer {

    private final boolean executorEnabled;
    private final ObservationMatcher observationMatcher;
    private final MethodExtensionMatcher methodExtensionMatcher;
    private final ClassEnhancementPlanner planner = new ClassEnhancementPlanner();
    private final DuplicateEnhancementDetector duplicateDetector =
            new DuplicateEnhancementDetector();
    private final ExecutorCallSiteEnhancer executorEnhancer = new ExecutorCallSiteEnhancer();
    private final MethodObservationEnhancer observationEnhancer =
            new MethodObservationEnhancer();
    private final MethodExtensionEnhancer methodExtensionEnhancer =
            new MethodExtensionEnhancer();

    public ApplicationClassEnhancer(
            boolean executorEnabled,
            ObservationMatcher observationMatcher
    ) {
        this(executorEnabled, observationMatcher, null);
    }

    public ApplicationClassEnhancer(
            boolean executorEnabled,
            ObservationMatcher observationMatcher,
            MethodExtensionMatcher methodExtensionMatcher
    ) {
        this.executorEnabled = executorEnabled;
        this.observationMatcher = observationMatcher;
        this.methodExtensionMatcher = methodExtensionMatcher;
    }

    public byte[] enhance(ClassLoader loader, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        ClassEnhancementPlan plan = planner.plan(
                loader, classNode, observationMatcher, methodExtensionMatcher);
        boolean changed = false;
        if (executorEnabled && !duplicateDetector.containsExecutorBridge(classNode)) {
            changed |= executorEnhancer.rewrite(loader, classNode);
        }
        if (observationMatcher != null
                && !duplicateDetector.containsObservationBridge(classNode)) {
            changed |= observationEnhancer.rewrite(loader, classNode, plan);
        }
        if (methodExtensionMatcher != null
                && !duplicateDetector.containsMethodExtensionBridge(classNode)) {
            changed |= methodExtensionEnhancer.rewrite(loader, classNode, plan);
        }
        if (!changed) {
            return null;
        }
        EgonClassWriter writer = new EgonClassWriter(
                reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
