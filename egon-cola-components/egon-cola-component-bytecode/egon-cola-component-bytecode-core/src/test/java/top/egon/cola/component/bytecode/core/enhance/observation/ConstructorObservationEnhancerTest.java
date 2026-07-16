package top.egon.cola.component.bytecode.core.enhance.observation;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import sample.bytecode.ConstructorObservationFixture;
import sample.bytecode.ObservationParentFixture;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.ObservationToken;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructorObservationEnhancerTest {

    @Test
    void startsOnlyAfterSuccessfulInitializationAndObservesEachConstructorBody()
            throws Exception {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        MethodObservationEnhancer enhancer = new MethodObservationEnhancer();
        ObservationMatcher matcher = new ObservationMatcher(
                List.of(ConstructorObservationFixture.class.getName()),
                List.of("no-ordinary-methods"), List.of(), true, 1_000L);

        byte[] transformed = enhancer.enhance(loader, fixtureBytes(), matcher);

        assertNotNull(transformed);
        StringWriter verification = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(transformed), loader, false,
                new PrintWriter(verification));
        assertEquals("", verification.toString());
        Class<?> type = loader.define(transformed);
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        try (var ignored = DispatcherRegistry.register(loader, "test", dispatcher)) {
            Object chained = type.getConstructor().newInstance();
            assertEquals("direct", type.getMethod("value").invoke(chained));
            assertTrue((Boolean) type.getMethod("outerBody").invoke(chained));
            assertEquals(2, dispatcher.successes);

            Object privateValue = type.getMethod("privateConstructor").invoke(null);
            assertEquals("private", type.getMethod("value").invoke(privateValue));
            assertEquals(3, dispatcher.successes);

            try {
                type.getConstructor(boolean.class, boolean.class)
                        .newInstance(true, false);
            } catch (InvocationTargetException exception) {
                assertSame(ObservationParentFixture.PARENT_FAILURE, exception.getCause());
            }
            assertEquals(3, dispatcher.enters, "parent failure must not enter observation");

            Throwable bodyFailure = (Throwable) type.getField("BODY_FAILURE").get(null);
            try {
                type.getConstructor(boolean.class, boolean.class)
                        .newInstance(false, true);
            } catch (InvocationTargetException exception) {
                assertSame(bodyFailure, exception.getCause());
            }
            assertEquals(4, dispatcher.enters);
            assertEquals(1, dispatcher.errors);
            assertEquals(4, dispatcher.exits);
        }
    }

    @Test
    void leavesUnannotatedConstructorsUnchangedByDefault() {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        ObservationMatcher matcher = new ObservationMatcher(
                List.of(ConstructorObservationFixture.class.getName()),
                List.of("no-ordinary-methods"), List.of(), false, 1_000L);

        assertNull(new MethodObservationEnhancer().enhance(loader, fixtureBytes(), matcher));
    }

    private byte[] fixtureBytes() {
        String resource = "/" + ConstructorObservationFixture.class.getName()
                .replace('.', '/') + ".class";
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
