package top.egon.cola.component.bytecode.core.enhance;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public final class DuplicateEnhancementDetector {

    private static final String EXECUTOR_BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonExecutorBridge";
    private static final String OBSERVATION_BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonObservationBridge";

    public boolean containsExecutorBridge(ClassNode classNode) {
        return classNode.methods.stream().anyMatch(method -> {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode invocation
                        && EXECUTOR_BRIDGE.equals(invocation.owner)
                        && ("execute".equals(invocation.name)
                        || "submit".equals(invocation.name))) {
                    return true;
                }
            }
            return false;
        });
    }

    public boolean containsObservationBridge(ClassNode classNode) {
        return classNode.methods.stream().anyMatch(method -> {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode invocation
                        && OBSERVATION_BRIDGE.equals(invocation.owner)
                        && "enter".equals(invocation.name)) {
                    return true;
                }
            }
            return false;
        });
    }
}
