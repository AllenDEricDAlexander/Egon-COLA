package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeConstructorInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.ConstructorGuardDecision;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.core.enhance.ApplicationClassEnhancer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructorGuardEnhancerTest {

    private static final String ACCESS_GUARD =
            "Ltop/egon/cola/component/accessguard/annotation/AccessGuard;";

    @Test
    void insertsVerifiedGuardBeforeAnyUseOfUninitializedThis() throws Exception {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        ApplicationClassEnhancer enhancer = new ApplicationClassEnhancer(
                false, null, null, new AccessGuardMatcher());
        byte[] transformed = enhancer.enhance(loader, fixtureBytes("FAIL_OPEN"));

        assertNotNull(transformed);
        assertVerified(loader, transformed);
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        MethodNode constructor = node.methods.stream()
                .filter(method -> method.name.equals("<init>"))
                .findFirst().orElseThrow();
        assertTrue(constructor.instructions.getFirst() instanceof LdcInsnNode);

        Class<?> type = loader.define(transformed);
        RejectingDispatcher dispatcher = new RejectingDispatcher();
        try (var ignored = DispatcherRegistry.register(loader, "test", dispatcher)) {
            InvocationTargetException rejected = assertThrows(InvocationTargetException.class,
                    () -> type.getConstructor(int.class).newInstance(7));
            assertEquals("constructor-rejected", rejected.getCause().getMessage());
            assertEquals(7, dispatcher.lastInvocation.arguments()[0]);
        }
        assertNotNull(type.getConstructor(int.class).newInstance(8));
    }

    @Test
    void honorsExplicitFailClosedWhenRuntimeIsAbsent() throws Exception {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        byte[] transformed = new ApplicationClassEnhancer(
                false, null, null, new AccessGuardMatcher())
                .enhance(loader, fixtureBytes("FAIL_CLOSED"));
        Class<?> type = loader.define(transformed);

        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> type.getConstructor(int.class).newInstance(1));
        assertTrue(failure.getCause() instanceof IllegalStateException);
    }

    private byte[] fixtureBytes(String failStrategy) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String owner = "sample/bytecode/ConstructorGuardFixture" + failStrategy;
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(
                Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
        AnnotationVisitor annotation = constructor.visitAnnotation(ACCESS_GUARD, true);
        annotation.visitEnum(
                "failStrategy",
                "Ltop/egon/cola/component/accessguard/annotation/FailStrategy;",
                failStrategy
        );
        annotation.visitEnd();
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private void assertVerified(ClassLoader loader, byte[] bytes) {
        StringWriter output = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(bytes), loader, false, new PrintWriter(output));
        assertEquals("", output.toString());
    }

    static final class DefiningClassLoader extends ClassLoader {

        DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    static final class RejectingDispatcher implements BytecodeRuntimeDispatcher {

        private BridgeConstructorInvocation lastInvocation;

        @Override
        public ConstructorGuardDecision guardConstructor(BridgeConstructorInvocation invocation) {
            lastInvocation = invocation;
            return ConstructorGuardDecision.throwing(
                    new IllegalStateException("constructor-rejected"));
        }

        @Override
        public int protocolMajor() {
            return BridgeProtocol.MAJOR;
        }

        @Override
        public int protocolMinor() {
            return BridgeProtocol.MINOR;
        }

        @Override
        public Set<BridgeCapability> capabilities() {
            return Set.of(BridgeCapability.ACCESS_GUARD);
        }

        @Override
        public Runnable decorateRunnable(
                Class<?> callerClass, Executor executor, Runnable task, long callSiteId) {
            return task;
        }

        @Override
        public <V> Callable<V> decorateCallable(
                Class<?> callerClass, Executor executor, Callable<V> task, long callSiteId) {
            return task;
        }
    }
}
