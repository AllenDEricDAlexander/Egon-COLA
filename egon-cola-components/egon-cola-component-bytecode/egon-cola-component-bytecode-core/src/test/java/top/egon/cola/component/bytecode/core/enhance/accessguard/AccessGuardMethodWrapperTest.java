package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.core.enhance.ApplicationClassEnhancer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGuardMethodWrapperTest {

    private static final String ACCESS_GUARD =
            "Ltop/egon/cola/component/accessguard/annotation/AccessGuard;";

    @Test
    void emitsVerifiedWrappersForInstancePrivateStaticAndSynchronizedMethods() throws Exception {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        ApplicationClassEnhancer enhancer = new ApplicationClassEnhancer(
                false, null, null, new AccessGuardMatcher());
        byte[] transformed = enhancer.enhance(loader, fixtureBytes());

        assertNotNull(transformed);
        assertVerified(loader, transformed);
        assertWrapperMetadata(transformed);
        assertNull(enhancer.enhance(loader, transformed));

        Class<?> type = loader.define(transformed);
        Object target = type.getConstructor().newInstance();
        GuardDispatcher dispatcher = new GuardDispatcher(loader);
        try (var ignored = DispatcherRegistry.register(loader, "test", dispatcher)) {
            assertEquals(3, type.getMethod("value", int.class).invoke(target, 2));
            assertSame(target, dispatcher.lastInvocation.target());
            assertEquals(2, dispatcher.lastInvocation.arguments()[0]);

            dispatcher.result("value", 41);
            assertEquals(41, type.getMethod("value", int.class).invoke(target, 2));
            assertEquals("private", type.getMethod("callHidden", String.class)
                    .invoke(target, "private"));
            assertEquals("static", type.getMethod("staticValue", String.class)
                    .invoke(null, "static"));
            assertNull(dispatcher.lastInvocation.target());

            IllegalStateException failure = new IllegalStateException("guard-sentinel");
            dispatcher.failure("staticValue", failure);
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                    () -> type.getMethod("staticValue", String.class).invoke(null, "value"));
            assertSame(failure, thrown.getCause());
        }
    }

    private void assertWrapperMetadata(byte[] transformed) {
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        MethodNode wrapper = node.methods.stream()
                .filter(method -> method.name.equals("value"))
                .findFirst().orElseThrow();
        MethodNode body = node.methods.stream()
                .filter(method -> method.name.startsWith("egon$guard$"))
                .filter(method -> method.desc.equals("(I)I"))
                .findFirst().orElseThrow();

        assertTrue(Modifier.isPublic(wrapper.access));
        assertTrue(Modifier.isSynchronized(wrapper.access));
        assertTrue(wrapper.visibleAnnotations.stream().anyMatch(annotation ->
                annotation.desc.equals(ACCESS_GUARD)));
        assertTrue(Modifier.isPrivate(body.access));
        assertTrue((body.access & Opcodes.ACC_SYNTHETIC) != 0);
        assertFalse(Modifier.isSynchronized(body.access));
        assertTrue(body.visibleAnnotations == null || body.visibleAnnotations.isEmpty());
    }

    private void assertVerified(ClassLoader loader, byte[] transformed) {
        StringWriter output = new StringWriter();
        CheckClassAdapter.verify(
                new ClassReader(transformed), loader, false, new PrintWriter(output));
        assertEquals("", output.toString());
    }

    private byte[] fixtureBytes() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String owner = "sample/bytecode/AccessGuardGeneratedFixture";
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);

        MethodVisitor constructor = writer.visitMethod(
                Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        MethodVisitor value = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED,
                "value", "(I)I", null, new String[]{"java/io/IOException"});
        annotation(value);
        value.visitCode();
        value.visitVarInsn(Opcodes.ILOAD, 1);
        value.visitInsn(Opcodes.ICONST_1);
        value.visitInsn(Opcodes.IADD);
        value.visitInsn(Opcodes.IRETURN);
        value.visitMaxs(0, 0);
        value.visitEnd();

        MethodVisitor hidden = writer.visitMethod(
                Opcodes.ACC_PRIVATE, "hidden", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        annotation(hidden);
        hidden.visitCode();
        hidden.visitVarInsn(Opcodes.ALOAD, 1);
        hidden.visitInsn(Opcodes.ARETURN);
        hidden.visitMaxs(0, 0);
        hidden.visitEnd();

        MethodVisitor callHidden = writer.visitMethod(
                Opcodes.ACC_PUBLIC, "callHidden", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        callHidden.visitCode();
        callHidden.visitVarInsn(Opcodes.ALOAD, 0);
        callHidden.visitVarInsn(Opcodes.ALOAD, 1);
        callHidden.visitMethodInsn(
                Opcodes.INVOKESPECIAL, owner, "hidden", "(Ljava/lang/String;)Ljava/lang/String;", false);
        callHidden.visitInsn(Opcodes.ARETURN);
        callHidden.visitMaxs(0, 0);
        callHidden.visitEnd();

        MethodVisitor staticValue = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "staticValue", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        annotation(staticValue);
        staticValue.visitCode();
        staticValue.visitVarInsn(Opcodes.ALOAD, 0);
        staticValue.visitInsn(Opcodes.ARETURN);
        staticValue.visitMaxs(0, 0);
        staticValue.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private void annotation(MethodVisitor method) {
        AnnotationVisitor annotation = method.visitAnnotation(ACCESS_GUARD, true);
        annotation.visitEnd();
    }

    static final class DefiningClassLoader extends ClassLoader {

        DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    static final class GuardDispatcher implements BytecodeRuntimeDispatcher {

        private final ClassLoader loader;
        private final Map<String, Object> results = new HashMap<>();
        private final Map<String, Throwable> failures = new HashMap<>();
        private BridgeGuardedInvocation lastInvocation;

        GuardDispatcher(ClassLoader loader) {
            this.loader = loader;
        }

        void result(String methodName, Object value) {
            results.put(methodName, value);
        }

        void failure(String methodName, Throwable failure) {
            failures.put(methodName, failure);
        }

        @Override
        public Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
            lastInvocation = invocation;
            String methodName = DispatcherRegistry.method(loader, invocation.methodId())
                    .orElseThrow().methodName();
            if (failures.containsKey(methodName)) {
                throw failures.get(methodName);
            }
            if (results.containsKey(methodName)) {
                return results.get(methodName);
            }
            return invocation.proceed();
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
