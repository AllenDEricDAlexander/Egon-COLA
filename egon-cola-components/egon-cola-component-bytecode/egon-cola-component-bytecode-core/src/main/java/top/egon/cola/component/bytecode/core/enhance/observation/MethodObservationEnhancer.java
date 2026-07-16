package top.egon.cola.component.bytecode.core.enhance.observation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.core.enhance.ClassEnhancementPlan;
import top.egon.cola.component.bytecode.core.enhance.ClassEnhancementPlanner;
import top.egon.cola.component.bytecode.core.enhance.DuplicateEnhancementDetector;
import top.egon.cola.component.bytecode.core.enhance.MethodEnhancementPlan;
import top.egon.cola.component.bytecode.core.hierarchy.EgonClassWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MethodObservationEnhancer {

    private static final String BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonObservationBridge";
    private static final String TOKEN =
            "top/egon/cola/component/bytecode/bridge/ObservationToken";
    private static final String TOKEN_DESCRIPTOR = "L" + TOKEN + ";";

    private final ClassEnhancementPlanner planner = new ClassEnhancementPlanner();
    private final DuplicateEnhancementDetector duplicateDetector =
            new DuplicateEnhancementDetector();
    private final ConstructorObservationEnhancer constructorEnhancer =
            new ConstructorObservationEnhancer();

    public byte[] enhance(
            ClassLoader loader,
            byte[] classfileBuffer,
            ObservationMatcher matcher
    ) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        if (duplicateDetector.containsObservationBridge(classNode)) {
            return null;
        }
        ClassEnhancementPlan plan = planner.plan(classNode, matcher);
        if (!rewrite(loader, classNode, plan)) {
            return null;
        }
        EgonClassWriter writer = new EgonClassWriter(
                reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    public boolean rewrite(
            ClassLoader loader,
            ClassNode classNode,
            ClassEnhancementPlan plan
    ) {
        Map<MethodKey, ObservationPolicy> policies = policies(plan);
        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            ObservationPolicy policy = policies.get(new MethodKey(method.name, method.desc));
            if (policy == null) {
                continue;
            }
            boolean rewritten;
            if (policy.constructor()) {
                rewritten = constructorEnhancer.rewriteConstructor(
                        classNode.name, classNode.superName, method, policy.methodId());
            } else {
                rewriteMethod(classNode.name, method, policy.methodId());
                rewritten = true;
            }
            if (!rewritten) {
                continue;
            }
            DispatcherRegistry.registerMethod(loader, policy.methodMetadata());
            DispatcherRegistry.registerObservation(loader, policy.observationMetadata());
            changed = true;
        }
        return changed;
    }

    private Map<MethodKey, ObservationPolicy> policies(ClassEnhancementPlan plan) {
        Map<MethodKey, ObservationPolicy> policies = new HashMap<>();
        for (MethodEnhancementPlan method : plan.methods()) {
            if (method.observationPolicy() != null) {
                policies.put(new MethodKey(method.methodName(), method.methodDescriptor()),
                        method.observationPolicy());
            }
        }
        return policies;
    }

    private void rewriteMethod(String owner, MethodNode method, long methodId) {
        List<AbstractInsnNode> returns = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() >= Opcodes.IRETURN
                    && instruction.getOpcode() <= Opcodes.RETURN) {
                returns.add(instruction);
            }
        }

        Type returnType = Type.getReturnType(method.desc);
        int tokenLocal = method.maxLocals;
        method.maxLocals += 1;
        int returnLocal = -1;
        if (returnType.getSort() != Type.VOID) {
            returnLocal = method.maxLocals;
            method.maxLocals += returnType.getSize();
        }
        int throwableLocal = method.maxLocals;
        method.maxLocals += 1;

        LabelNode originalStart = new LabelNode();
        LabelNode originalEnd = new LabelNode();
        LabelNode normalExit = new LabelNode();
        LabelNode errorExit = new LabelNode();

        InsnList prologue = new InsnList();
        prologue.add(new LdcInsnNode(Type.getObjectType(owner)));
        prologue.add(new LdcInsnNode(methodId));
        prologue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE,
                "enter",
                "(Ljava/lang/Class;J)" + TOKEN_DESCRIPTOR,
                false
        ));
        prologue.add(new VarInsnNode(Opcodes.ASTORE, tokenLocal));
        prologue.add(originalStart);
        method.instructions.insert(prologue);

        for (AbstractInsnNode returnInstruction : returns) {
            InsnList replacement = new InsnList();
            if (returnType.getSort() != Type.VOID) {
                replacement.add(new VarInsnNode(
                        returnType.getOpcode(Opcodes.ISTORE), returnLocal));
            }
            replacement.add(new JumpInsnNode(Opcodes.GOTO, normalExit));
            method.instructions.insertBefore(returnInstruction, replacement);
            method.instructions.remove(returnInstruction);
        }

        InsnList epilogue = new InsnList();
        epilogue.add(originalEnd);
        epilogue.add(normalExit);
        epilogue.add(new VarInsnNode(Opcodes.ALOAD, tokenLocal));
        epilogue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE,
                "success",
                "(" + TOKEN_DESCRIPTOR + ")V",
                false
        ));
        epilogue.add(new VarInsnNode(Opcodes.ALOAD, tokenLocal));
        epilogue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE,
                "exit",
                "(" + TOKEN_DESCRIPTOR + ")V",
                false
        ));
        if (returnType.getSort() == Type.VOID) {
            epilogue.add(new InsnNode(Opcodes.RETURN));
        } else {
            epilogue.add(new VarInsnNode(
                    returnType.getOpcode(Opcodes.ILOAD), returnLocal));
            epilogue.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        }
        epilogue.add(errorExit);
        epilogue.add(new VarInsnNode(Opcodes.ASTORE, throwableLocal));
        epilogue.add(new VarInsnNode(Opcodes.ALOAD, tokenLocal));
        epilogue.add(new VarInsnNode(Opcodes.ALOAD, throwableLocal));
        epilogue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE,
                "error",
                "(" + TOKEN_DESCRIPTOR + "Ljava/lang/Throwable;)V",
                false
        ));
        epilogue.add(new VarInsnNode(Opcodes.ALOAD, tokenLocal));
        epilogue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE,
                "exit",
                "(" + TOKEN_DESCRIPTOR + ")V",
                false
        ));
        epilogue.add(new VarInsnNode(Opcodes.ALOAD, throwableLocal));
        epilogue.add(new InsnNode(Opcodes.ATHROW));
        method.instructions.add(epilogue);
        method.tryCatchBlocks.add(new TryCatchBlockNode(
                originalStart, originalEnd, errorExit, "java/lang/Throwable"));
    }

    private record MethodKey(String name, String descriptor) {
    }
}
