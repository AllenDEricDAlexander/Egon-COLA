package top.egon.cola.component.bytecode.core.enhance.executor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.bridge.CallSiteMetadata;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.core.enhance.ClassEnhancementPlan;
import top.egon.cola.component.bytecode.core.enhance.ClassEnhancementPlanner;
import top.egon.cola.component.bytecode.core.enhance.DuplicateEnhancementDetector;
import top.egon.cola.component.bytecode.core.hierarchy.EgonClassWriter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class ExecutorCallSiteEnhancer {

    private static final String EXECUTOR = "java/util/concurrent/Executor";
    private static final String EXECUTOR_SERVICE = "java/util/concurrent/ExecutorService";
    private static final String BRIDGE =
            "top/egon/cola/component/bytecode/bridge/EgonExecutorBridge";
    private static final Map<InvocationSignature, String> BRIDGE_DESCRIPTORS = descriptors();

    private final Function<CallSiteIdInput, Long> idGenerator;
    private final ClassEnhancementPlanner planner = new ClassEnhancementPlanner();
    private final DuplicateEnhancementDetector duplicateDetector = new DuplicateEnhancementDetector();

    public ExecutorCallSiteEnhancer() {
        this(ExecutorCallSiteEnhancer::stableId);
    }

    public ExecutorCallSiteEnhancer(Function<CallSiteIdInput, Long> idGenerator) {
        this.idGenerator = idGenerator;
    }

    public byte[] enhance(ClassLoader loader, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        if (duplicateDetector.containsExecutorBridge(classNode)) {
            return null;
        }
        ClassEnhancementPlan plan = planner.plan(classNode);
        if (plan.empty()) {
            return null;
        }
        boolean changed = rewrite(loader, classNode);
        if (!changed) {
            return null;
        }
        EgonClassWriter writer = new EgonClassWriter(
                reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    public static boolean supports(MethodInsnNode invocation) {
        return invocation.getOpcode() == Opcodes.INVOKEINTERFACE
                && invocation.itf
                && BRIDGE_DESCRIPTORS.containsKey(new InvocationSignature(
                invocation.owner, invocation.name, invocation.desc));
    }

    public boolean rewrite(ClassLoader loader, ClassNode classNode) {
        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            Integer lineNumber = null;
            int ordinal = 0;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null; instruction = instruction.getNext(), ordinal++) {
                if (instruction instanceof LineNumberNode line) {
                    lineNumber = line.line;
                }
                if (!(instruction instanceof MethodInsnNode invocation)
                        || !supports(invocation)) {
                    continue;
                }
                CallSiteIdInput input = new CallSiteIdInput(
                        classNode.name,
                        method.name,
                        method.desc,
                        invocation.getOpcode(),
                        invocation.owner,
                        invocation.name,
                        invocation.desc,
                        ordinal
                );
                long callSiteId = idGenerator.apply(input);
                DispatcherRegistry.registerCallSite(loader, new CallSiteMetadata(
                        callSiteId,
                        classNode.name,
                        method.name,
                        method.desc,
                        invocation.owner,
                        invocation.name,
                        invocation.desc,
                        lineNumber
                ));
                InsnList suffix = new InsnList();
                suffix.add(new LdcInsnNode(Type.getObjectType(classNode.name)));
                suffix.add(new LdcInsnNode(callSiteId));
                method.instructions.insertBefore(invocation, suffix);
                invocation.setOpcode(Opcodes.INVOKESTATIC);
                invocation.owner = BRIDGE;
                invocation.desc = BRIDGE_DESCRIPTORS.get(new InvocationSignature(
                        input.targetOwner(), input.targetName(), input.targetDescriptor()));
                invocation.itf = false;
                changed = true;
            }
        }
        return changed;
    }

    private static long stableId(CallSiteIdInput input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, input.owner());
            update(digest, input.enclosingMethodName());
            update(digest, input.enclosingMethodDescriptor());
            update(digest, Integer.toString(input.opcode()));
            update(digest, input.targetOwner());
            update(digest, input.targetName());
            update(digest, input.targetDescriptor());
            update(digest, Integer.toString(input.instructionOrdinal()));
            return ByteBuffer.wrap(digest.digest()).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static Map<InvocationSignature, String> descriptors() {
        Map<InvocationSignature, String> descriptors = new LinkedHashMap<>();
        descriptors.put(new InvocationSignature(
                        EXECUTOR,
                        "execute",
                        "(Ljava/lang/Runnable;)V"),
                "(Ljava/util/concurrent/Executor;Ljava/lang/Runnable;Ljava/lang/Class;J)V");
        descriptors.put(new InvocationSignature(
                        EXECUTOR_SERVICE,
                        "submit",
                        "(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;"),
                "(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;"
                        + "Ljava/lang/Class;J)Ljava/util/concurrent/Future;");
        descriptors.put(new InvocationSignature(
                        EXECUTOR_SERVICE,
                        "submit",
                        "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;"),
                "(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;Ljava/lang/Object;"
                        + "Ljava/lang/Class;J)Ljava/util/concurrent/Future;");
        descriptors.put(new InvocationSignature(
                        EXECUTOR_SERVICE,
                        "submit",
                        "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Future;"),
                "(Ljava/util/concurrent/ExecutorService;Ljava/util/concurrent/Callable;"
                        + "Ljava/lang/Class;J)Ljava/util/concurrent/Future;");
        return Map.copyOf(descriptors);
    }

    private record InvocationSignature(String owner, String name, String descriptor) {
    }
}
