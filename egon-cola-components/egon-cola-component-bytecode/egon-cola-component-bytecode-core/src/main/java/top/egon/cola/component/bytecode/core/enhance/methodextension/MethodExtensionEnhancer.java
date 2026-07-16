package top.egon.cola.component.bytecode.core.enhance.methodextension;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
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

public final class MethodExtensionEnhancer {

    private static final String POLICY_BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonPolicyBridge";
    private static final String DECISION =
            "top/egon/cola/component/bytecode/bridge/InvocationDecision";
    private static final String DECISION_KIND =
            "top/egon/cola/component/bytecode/bridge/DecisionKind";

    public boolean rewrite(
            ClassLoader loader,
            ClassNode classNode,
            ClassEnhancementPlan plan
    ) {
        Map<MethodKey, MethodEnhancementPlan> methods = methods(plan);
        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            MethodEnhancementPlan methodPlan = methods.get(new MethodKey(method.name, method.desc));
            if (methodPlan == null || methodPlan.methodExtensionPolicy() == null) {
                continue;
            }
            rewriteMethod(classNode.name, method, methodPlan.methodExtensionPolicy().methodId());
            DispatcherRegistry.registerMethod(loader,
                    methodPlan.methodExtensionPolicy()
                            .methodMetadata(methodPlan.bridgeCapabilities()));
            changed = true;
        }
        return changed;
    }

    private Map<MethodKey, MethodEnhancementPlan> methods(ClassEnhancementPlan plan) {
        Map<MethodKey, MethodEnhancementPlan> methods = new HashMap<>();
        for (MethodEnhancementPlan method : plan.methods()) {
            if (method.methodExtensionPolicy() != null) {
                methods.put(new MethodKey(method.methodName(), method.methodDescriptor()), method);
            }
        }
        return methods;
    }

    private void rewriteMethod(String owner, MethodNode method, long methodId) {
        int decisionLocal = method.maxLocals;
        method.maxLocals++;
        LabelNode proceed = new LabelNode();
        LabelNode returnNull = new LabelNode();
        LabelNode returnValue = new LabelNode();
        InsnList entry = new InsnList();
        entry.add(new VarInsnNode(Opcodes.ALOAD, 0));
        entry.add(new LdcInsnNode(Type.getObjectType(owner)));
        entry.add(new LdcInsnNode(methodId));
        appendArguments(entry, method.desc);
        entry.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                POLICY_BRIDGE,
                "evaluateMethodExtension",
                "(Ljava/lang/Object;Ljava/lang/Class;J[Ljava/lang/Object;)L" + DECISION + ";",
                false
        ));
        entry.add(new VarInsnNode(Opcodes.ASTORE, decisionLocal));
        appendKindBranch(entry, decisionLocal, "PROCEED", proceed);
        appendKindBranch(entry, decisionLocal, "RETURN_NULL", returnNull);
        appendKindBranch(entry, decisionLocal, "RETURN_VALUE", returnValue);
        entry.add(new VarInsnNode(Opcodes.ALOAD, decisionLocal));
        entry.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                DECISION,
                "throwable",
                "()Ljava/lang/Throwable;",
                false
        ));
        entry.add(new InsnNode(Opcodes.ATHROW));
        entry.add(returnNull);
        appendNullReturn(entry, Type.getReturnType(method.desc));
        entry.add(returnValue);
        appendValueReturn(entry, decisionLocal, Type.getReturnType(method.desc));
        entry.add(proceed);
        method.instructions.insert(entry);
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

    private void appendKindBranch(
            InsnList instructions,
            int decisionLocal,
            String kind,
            LabelNode target
    ) {
        instructions.add(new VarInsnNode(Opcodes.ALOAD, decisionLocal));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                DECISION,
                "kind",
                "()L" + DECISION_KIND + ";",
                false
        ));
        instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                DECISION_KIND,
                kind,
                "L" + DECISION_KIND + ";"
        ));
        instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, target));
    }

    private void appendNullReturn(InsnList instructions, Type returnType) {
        if (returnType.getSort() == Type.VOID) {
            instructions.add(new InsnNode(Opcodes.RETURN));
            return;
        }
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            return;
        }
        appendConfigurationFailure(
                instructions,
                "Method Extension RETURN_NULL cannot target a primitive return type"
        );
    }

    private void appendValueReturn(InsnList instructions, int decisionLocal, Type returnType) {
        if (returnType.getSort() == Type.VOID) {
            appendConfigurationFailure(
                    instructions,
                    "Method Extension RETURN_VALUE cannot target a void return type"
            );
            return;
        }
        instructions.add(new VarInsnNode(Opcodes.ALOAD, decisionLocal));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                DECISION,
                "value",
                "()Ljava/lang/Object;",
                false
        ));
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            return;
        }
        unbox(instructions, returnType);
        instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
    }

    private void appendConfigurationFailure(InsnList instructions, String message) {
        instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new LdcInsnNode(message));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/IllegalStateException",
                "<init>",
                "(Ljava/lang/String;)V",
                false
        ));
        instructions.add(new InsnNode(Opcodes.ATHROW));
    }

    private void box(InsnList instructions, Type type) {
        String wrapper = wrapper(type);
        if (wrapper == null) {
            return;
        }
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                wrapper,
                "valueOf",
                "(" + type.getDescriptor() + ")L" + wrapper + ";",
                false
        ));
    }

    private void unbox(InsnList instructions, Type type) {
        String wrapper = wrapper(type);
        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, wrapper));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                wrapper,
                primitiveValueMethod(type),
                "()" + type.getDescriptor(),
                false
        ));
    }

    private String wrapper(Type type) {
        return switch (type.getSort()) {
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
    }

    private String primitiveValueMethod(Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN -> "booleanValue";
            case Type.BYTE -> "byteValue";
            case Type.CHAR -> "charValue";
            case Type.SHORT -> "shortValue";
            case Type.INT -> "intValue";
            case Type.FLOAT -> "floatValue";
            case Type.LONG -> "longValue";
            case Type.DOUBLE -> "doubleValue";
            default -> throw new IllegalArgumentException("Not a primitive type: " + type);
        };
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

    private record MethodKey(String name, String descriptor) {
    }
}
