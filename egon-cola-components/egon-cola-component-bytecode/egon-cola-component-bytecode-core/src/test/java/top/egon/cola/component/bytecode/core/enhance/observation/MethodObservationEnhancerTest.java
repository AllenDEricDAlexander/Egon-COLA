package top.egon.cola.component.bytecode.core.enhance.observation;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import sample.bytecode.ObservationFixture;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.ObservationToken;
import top.egon.cola.component.bytecode.core.enhance.ApplicationClassEnhancer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MethodObservationEnhancerTest {

    @Test
    void preservesAllReturnsControlFlowAndExactThrowable() throws Exception {
        byte[] original = fixtureBytes();
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        ObservationMatcher matcher = new ObservationMatcher(
                List.of(ObservationFixture.class.getName()), List.of("*"), List.of(),
                false, 1_000L);
        MethodObservationEnhancer enhancer = new MethodObservationEnhancer();

        byte[] transformed = enhancer.enhance(loader, original, matcher);

        assertNotNull(transformed);
        StringWriter verification = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(transformed), loader, false,
                new PrintWriter(verification));
        assertEquals("", verification.toString());
        Class<?> type = loader.define(transformed);
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        try (var ignored = DispatcherRegistry.register(loader, "test", dispatcher)) {
            Object fixture = type.getConstructor().newInstance();
            assertEquals(7, type.getMethod("integer", int.class).invoke(fixture, 7));
            assertEquals(9L, type.getMethod("longValue", long.class).invoke(fixture, 9L));
            assertEquals(2.5D, type.getMethod("doubleValue", double.class).invoke(fixture, 2.5D));
            assertEquals(3.5F, type.getMethod("floatValue", float.class).invoke(fixture, 3.5F));
            assertEquals(true, type.getMethod("booleanValue", boolean.class).invoke(fixture, true));
            assertEquals('x', type.getMethod("charValue", char.class).invoke(fixture, 'x'));
            Object reference = new Object();
            assertSame(reference, type.getMethod("reference", Object.class)
                    .invoke(fixture, reference));
            assertEquals("private", type.getMethod("sameClassCall").invoke(fixture));
            assertEquals(6, type.getMethod("recurse", int.class).invoke(fixture, 3));
            assertEquals(1, type.getMethod("tryFinally", boolean.class).invoke(fixture, false));
            type.getMethod("voidValue").invoke(fixture);
            assertEquals("static", type.getMethod("staticValue").invoke(null));

            IllegalStateException originalFailure = new IllegalStateException("same-instance");
            try {
                type.getMethod("failure", IllegalStateException.class)
                        .invoke(fixture, originalFailure);
            } catch (InvocationTargetException exception) {
                assertSame(originalFailure, exception.getCause());
            }
        }
        assertEquals(dispatcher.enters, dispatcher.exits);
        assertEquals(1, dispatcher.errors);
        assertEquals(dispatcher.enters - dispatcher.errors, dispatcher.successes);
        assertNull(enhancer.enhance(loader, transformed, matcher));
    }

    @Test
    void composesExecutorAndObservationInOneClassEnhancement() {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        ObservationMatcher matcher = new ObservationMatcher(
                List.of(ObservationFixture.class.getName()), List.of("*"), List.of(),
                false, 1_000L);

        byte[] transformed = new ApplicationClassEnhancer(true, matcher)
                .enhance(loader, fixtureBytes());

        assertNotNull(transformed);
        AtomicInteger executorCalls = new AtomicInteger();
        AtomicInteger observationCalls = new AtomicInteger();
        new ClassReader(transformed).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (owner.endsWith("/EgonExecutorBridge")) {
                            executorCalls.incrementAndGet();
                        }
                        if (owner.endsWith("/EgonObservationBridge")
                                && "enter".equals(methodName)) {
                            observationCalls.incrementAndGet();
                        }
                    }
                };
            }
        }, 0);
        assertEquals(1, executorCalls.get());
        assertEquals(15, observationCalls.get());
    }

    private byte[] fixtureBytes() {
        String resource = "/" + ObservationFixture.class.getName().replace('.', '/') + ".class";
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream);
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static final class DefiningClassLoader extends ClassLoader {

        DefiningClassLoader(ClassLoader parent) { super(parent); }

        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }

    static final class RecordingDispatcher implements BytecodeRuntimeDispatcher {

        int enters;
        int successes;
        int errors;
        int exits;

        @Override
        public int protocolMajor() { return BridgeProtocol.MAJOR; }

        @Override
        public int protocolMinor() { return BridgeProtocol.MINOR; }

        @Override
        public Set<BridgeCapability> capabilities() {
            return Set.of(BridgeCapability.OBSERVATION);
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

        @Override
        public ObservationToken enterObservation(Class<?> declaringClass, long methodId) {
            enters++;
            return ObservationToken.active(this, new Object());
        }

        @Override
        public void observationSuccess(ObservationToken token) { successes++; }

        @Override
        public void observationError(ObservationToken token, Throwable throwable) { errors++; }

        @Override
        public void observationExit(ObservationToken token) { exits++; }
    }
}
