package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class AccessGuardMethodWrapper {

    private static final String INVOCATION =
            "top/egon/cola/component/bytecode/bridge/BridgeGuardedInvocation";
    private static final String POLICY_BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonPolicyBridge";

    public boolean rewrite(
            ClassLoader loader,
            ClassNode classNode,
            ClassEnhancementPlan plan
    ) {
        Map<MethodKey, MethodEnhancementPlan> policies = policies(plan);
        boolean changed = false;
        for (MethodNode method : new ArrayList<>(classNode.methods)) {
            MethodEnhancementPlan methodPlan = policies.get(new MethodKey(method.name, method.desc));
            if (methodPlan == null || methodPlan.accessGuardPolicy() == null) {
                continue;
            }
            AccessGuardPolicy policy = methodPlan.accessGuardPolicy();
            String bodyName = SyntheticBodyName.from(policy.methodId());
            ensureAvailable(classNode, bodyName, method.desc);
            MethodNode body = moveBody(method, bodyName);
            classNode.methods.add(body);
            writeWrapper(classNode.name, method, bodyName, policy.methodId());
            DispatcherRegistry.registerMethod(
                    loader, policy.methodMetadata(methodPlan.bridgeCapabilities()));
            changed = true;
        }
        return changed;
    }

    private Map<MethodKey, MethodEnhancementPlan> policies(ClassEnhancementPlan plan) {
        Map<MethodKey, MethodEnhancementPlan> result = new HashMap<>();
        for (MethodEnhancementPlan method : plan.methods()) {
            if (method.accessGuardPolicy() != null) {
                result.put(new MethodKey(method.methodName(), method.methodDescriptor()), method);
            }
        }
        return result;
    }

    private void ensureAvailable(ClassNode classNode, String bodyName, String descriptor) {
        if (classNode.methods.stream().anyMatch(method ->
                method.name.equals(bodyName) && method.desc.equals(descriptor))) {
            throw new IllegalArgumentException(
                    "Access Guard synthetic body name collision: " + bodyName + descriptor);
        }
    }

    private MethodNode moveBody(MethodNode wrapper, String bodyName) {
        int bodyAccess = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC
                | (wrapper.access & (Opcodes.ACC_STATIC | Opcodes.ACC_STRICT));
        MethodNode body = new MethodNode(
                Opcodes.ASM9,
                bodyAccess,
                bodyName,
                wrapper.desc,
                null,
                wrapper.exceptions == null ? null : wrapper.exceptions.toArray(String[]::new)
        );
        body.instructions = wrapper.instructions;
        body.tryCatchBlocks = wrapper.tryCatchBlocks;
        body.localVariables = wrapper.localVariables;
        body.visibleLocalVariableAnnotations = wrapper.visibleLocalVariableAnnotations;
        body.invisibleLocalVariableAnnotations = wrapper.invisibleLocalVariableAnnotations;
        body.maxStack = wrapper.maxStack;
        body.maxLocals = wrapper.maxLocals;

        wrapper.instructions = new InsnList();
        wrapper.tryCatchBlocks = new ArrayList<>();
        wrapper.localVariables = null;
        wrapper.visibleLocalVariableAnnotations = null;
        wrapper.invisibleLocalVariableAnnotations = null;
        wrapper.maxStack = 0;
        wrapper.maxLocals = 0;
        return body;
    }

    private void writeWrapper(String owner, MethodNode method, String bodyName, long methodId) {
        boolean staticMethod = (method.access & Opcodes.ACC_STATIC) != 0;
        InsnList instructions = method.instructions;
        instructions.add(new TypeInsnNode(Opcodes.NEW, INVOCATION));
        instructions.add(new InsnNode(Opcodes.DUP));
        if (staticMethod) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        instructions.add(new LdcInsnNode(Type.getObjectType(owner)));
        instructions.add(new LdcInsnNode(methodId));
        appendArguments(instructions, method.desc, staticMethod);
        instructions.add(new LdcInsnNode(new Handle(
                staticMethod ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKESPECIAL,
                owner,
                bodyName,
                method.desc,
                false
        )));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                INVOCATION,
                "<init>",
                "(Ljava/lang/Object;Ljava/lang/Class;J[Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)V",
                false
        ));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                POLICY_BRIDGE,
                "invokeGuarded",
                "(L" + INVOCATION + ";)Ljava/lang/Object;",
                false
        ));
        appendReturn(instructions, Type.getReturnType(method.desc));
    }

    private void appendArguments(InsnList instructions, String descriptor, boolean staticMethod) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        pushInteger(instructions, argumentTypes.length);
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        int local = staticMethod ? 0 : 1;
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

    private void appendReturn(InsnList instructions, Type returnType) {
        if (returnType.getSort() == Type.VOID) {
            instructions.add(new InsnNode(Opcodes.POP));
            instructions.add(new InsnNode(Opcodes.RETURN));
            return;
        }
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            return;
        }
        unbox(instructions, returnType);
        instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
    }

    private void box(InsnList instructions, Type type) {
        String wrapper = wrapper(type);
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
