package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.core.enhance.ClassEnhancementPlan;
import top.egon.cola.component.bytecode.core.enhance.MethodEnhancementPlan;

import java.util.HashMap;
import java.util.Map;

public final class ConstructorGuardEnhancer {

    private static final String POLICY_BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonPolicyBridge";
    private static final String FAIL_HINT =
            "top/egon/cola/component/bytecode/bridge/BridgeFailHint";
    private static final String DECISION =
            "top/egon/cola/component/bytecode/bridge/ConstructorGuardDecision";

    public boolean rewrite(
            ClassLoader loader,
            ClassNode classNode,
            ClassEnhancementPlan plan
    ) {
        Map<MethodKey, MethodEnhancementPlan> policies = policies(plan);
        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            MethodEnhancementPlan methodPlan = policies.get(new MethodKey(method.name, method.desc));
            if (methodPlan == null || methodPlan.accessGuardPolicy() == null
                    || !methodPlan.accessGuardPolicy().constructor()) {
                continue;
            }
            AccessGuardPolicy policy = methodPlan.accessGuardPolicy();
            method.instructions.insert(constructorGuard(classNode.name, method, policy));
            DispatcherRegistry.registerMethod(
                    loader, policy.methodMetadata(methodPlan.bridgeCapabilities()));
            changed = true;
        }
        return changed;
    }

    private InsnList constructorGuard(
            String owner,
            MethodNode constructor,
            AccessGuardPolicy policy
    ) {
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(Type.getObjectType(owner)));
        instructions.add(new LdcInsnNode(policy.methodId()));
        appendArguments(instructions, constructor.desc);
        instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                FAIL_HINT,
                policy.constructorFailHint().name(),
                "L" + FAIL_HINT + ";"
        ));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                POLICY_BRIDGE,
                "guardConstructor",
                "(Ljava/lang/Class;J[Ljava/lang/Object;L" + FAIL_HINT + ";)L" + DECISION + ";",
                false
        ));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                DECISION,
                "throwIfRejected",
                "()V",
                false
        ));
        return instructions;
    }

    private void appendArguments(InsnList instructions, String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        pushInteger(instructions, argumentTypes.length);
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        int local = 1;
        for (int index = 0; index < argumentTypes.length; index++) {
            Type type = argumentTypes[index];
            instructions.add(new InsnNode(Opcodes.DUP));
            pushInteger(instructions, index);
            instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), local));
            box(instructions, type);
            instructions.add(new InsnNode(Opcodes.AASTORE));
            local += type.getSize();
        }
    }

    private void box(InsnList instructions, Type type) {
        String wrapper = switch (type.getSort()) {
            case Type.BOOLEAN -> "java/lang/Boolean";
            case Type.BYTE -> "java/lang/Byte";
            case Type.CHAR -> "java/lang/Character";
            case Type.SHORT -> "java/lang/Short";
            case Type.INT -> "java/lang/Integer";
            case Type.FLOAT -> "java/lang/Float";
            case Type.LONG -> "java/lang/Long";
            case Type.DOUBLE -> "java/lang/Double";
            default -> null;
        };
        if (wrapper != null) {
            instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    wrapper,
                    "valueOf",
                    "(" + type.getDescriptor() + ")L" + wrapper + ";",
                    false
            ));
        }
    }

    private void pushInteger(InsnList instructions, int value) {
        if (value >= -1 && value <= 5) {
            instructions.add(new InsnNode(Opcodes.ICONST_0 + value));
        } else if (value <= Byte.MAX_VALUE) {
            instructions.add(new IntInsnNode(Opcodes.BIPUSH, value));
        } else if (value <= Short.MAX_VALUE) {
            instructions.add(new IntInsnNode(Opcodes.SIPUSH, value));
        } else {
            instructions.add(new LdcInsnNode(value));
        }
    }

    private Map<MethodKey, MethodEnhancementPlan> policies(ClassEnhancementPlan plan) {
        Map<MethodKey, MethodEnhancementPlan> result = new HashMap<>();
        for (MethodEnhancementPlan method : plan.methods()) {
            if (method.accessGuardPolicy() != null
                    && method.accessGuardPolicy().constructor()) {
                result.put(new MethodKey(method.methodName(), method.methodDescriptor()), method);
            }
        }
        return result;
    }

    private record MethodKey(String name, String descriptor) {
    }
}
