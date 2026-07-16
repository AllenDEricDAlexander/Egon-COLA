package top.egon.cola.component.bytecode.core.enhance.observation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class ConstructorObservationEnhancer {

    private static final String BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonObservationBridge";
    private static final String TOKEN =
            "top/egon/cola/component/bytecode/bridge/ObservationToken";
    private static final String TOKEN_DESCRIPTOR = "L" + TOKEN + ";";

    public boolean rewriteConstructor(
            String owner,
            String directSuperName,
            MethodNode method,
            long methodId
    ) {
        MethodInsnNode initialization = initialization(owner, directSuperName, method);
        if (initialization == null) {
            return false;
        }
        List<AbstractInsnNode> returns = returnsAfter(initialization);
        if (returns.isEmpty()) {
            return false;
        }

        int tokenLocal = method.maxLocals;
        method.maxLocals += 1;
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
        method.instructions.insert(initialization, prologue);

        for (AbstractInsnNode returnInstruction : returns) {
            method.instructions.insertBefore(returnInstruction,
                    new JumpInsnNode(Opcodes.GOTO, normalExit));
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
        epilogue.add(new InsnNode(Opcodes.RETURN));
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
        return true;
    }

    private MethodInsnNode initialization(
            String owner,
            String directSuperName,
            MethodNode method
    ) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode invocation
                    && invocation.getOpcode() == Opcodes.INVOKESPECIAL
                    && "<init>".equals(invocation.name)
                    && (owner.equals(invocation.owner)
                    || directSuperName.equals(invocation.owner))) {
                return invocation;
            }
        }
        return null;
    }

    private List<AbstractInsnNode> returnsAfter(AbstractInsnNode initialization) {
        List<AbstractInsnNode> returns = new ArrayList<>();
        for (AbstractInsnNode instruction = initialization.getNext();
             instruction != null; instruction = instruction.getNext()) {
            if (instruction.getOpcode() == Opcodes.RETURN) {
                returns.add(instruction);
            }
        }
        return returns;
    }
}
