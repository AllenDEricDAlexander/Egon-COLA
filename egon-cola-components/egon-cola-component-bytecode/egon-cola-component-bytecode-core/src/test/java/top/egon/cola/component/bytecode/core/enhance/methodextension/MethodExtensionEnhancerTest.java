package top.egon.cola.component.bytecode.core.enhance.methodextension;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import sample.bytecode.MethodExtensionFixture;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeMethodInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.InvocationDecision;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.core.enhance.ApplicationClassEnhancer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodExtensionEnhancerTest {

    @Test
    void emitsVerifiedTypedDecisionsForEverySupportedReturnKind() throws Exception {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        byte[] transformed = new ApplicationClassEnhancer(
                false, null, new MethodExtensionMatcher()).enhance(loader, fixtureBytes());

        assertNotNull(transformed);
        StringWriter verification = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(transformed), loader, false,
                new PrintWriter(verification));
        assertEquals("", verification.toString());
        Class<?> type = loader.define(transformed);
        Object fixture = type.getConstructor().newInstance();
        DecisionDispatcher dispatcher = new DecisionDispatcher(loader);
        try (var ignored = DispatcherRegistry.register(loader, "test", dispatcher)) {
            dispatcher.decide("primitive", InvocationDecision.proceed());
            assertEquals(8, type.getMethod("primitive", int.class).invoke(fixture, 7));

            dispatcher.decide("primitive", InvocationDecision.returnValue(41));
            assertEquals(41, type.getMethod("primitive", int.class).invoke(fixture, 7));
            assertEquals(7, dispatcher.lastInvocation.arguments()[0]);
            assertSame(fixture, dispatcher.lastInvocation.target());

            dispatcher.decide("reference", InvocationDecision.returnValue("rejected"));
            assertEquals("rejected", type.getMethod("reference", String.class)
                    .invoke(fixture, "value"));
            dispatcher.decide("reference", InvocationDecision.returnNull());
            assertNull(type.getMethod("reference", String.class).invoke(fixture, "value"));

            dispatcher.decide("voidValue", InvocationDecision.returnNull());
            type.getMethod("voidValue").invoke(fixture);

            dispatcher.decide("privateValue", InvocationDecision.returnValue("private-short"));
            assertEquals("private-short", type.getMethod("callPrivate").invoke(fixture));
            dispatcher.decide("protectedValue", InvocationDecision.returnValue("protected-short"));
            assertEquals("protected-short", type.getMethod("callProtected").invoke(fixture));
            dispatcher.decide("packageValue", InvocationDecision.returnValue("package-short"));
            assertEquals("package-short", type.getMethod("callPackage").invoke(fixture));
            dispatcher.decide("inherited", InvocationDecision.returnValue("interface-short"));
            assertEquals("interface-short", type.getMethod("inherited", String.class)
                    .invoke(fixture, "value"));

            IllegalStateException failure = new IllegalStateException("same-instance");
            dispatcher.decide("finalSynchronized", InvocationDecision.throwing(failure));
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                    () -> type.getMethod("finalSynchronized").invoke(fixture));
            assertSame(failure, thrown.getCause());

            dispatcher.decide("primitive", InvocationDecision.returnNull());
            InvocationTargetException primitiveNull = assertThrows(InvocationTargetException.class,
                    () -> type.getMethod("primitive", int.class).invoke(fixture, 1));
            assertTrue(primitiveNull.getCause() instanceof IllegalStateException);
        }
        assertEquals(1, type.getMethod("calls").invoke(fixture));
    }

    @Test
    void recordsFeatureMetadataAndPreventsDuplicateEnhancement() {
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        ApplicationClassEnhancer enhancer = new ApplicationClassEnhancer(
                false, null, new MethodExtensionMatcher());

        byte[] transformed = enhancer.enhance(loader, fixtureBytes());

        assertNotNull(transformed);
        assertNull(enhancer.enhance(loader, transformed));
        assertTrue(DispatcherRegistry.status(loader).methodCount() >= 8);
        MethodMetadata metadata = DispatcherRegistry.method(
                loader,
                top.egon.cola.component.bytecode.core.enhance.MethodId.compute(
                        "sample/bytecode/MethodExtensionFixture", "privateValue",
                        "()Ljava/lang/String;"))
                .orElseThrow();
        assertEquals(Set.of(BridgeCapability.METHOD_EXTENSION), metadata.features());
    }

    private byte[] fixtureBytes() {
        String resource = "/" + MethodExtensionFixture.class.getName()
                .replace('.', '/') + ".class";
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream);
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static final class DefiningClassLoader extends ClassLoader {

        DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    static final class DecisionDispatcher implements BytecodeRuntimeDispatcher {

        private final ClassLoader loader;
        private final Map<String, InvocationDecision> decisions = new HashMap<>();
        private BridgeMethodInvocation lastInvocation;

        DecisionDispatcher(ClassLoader loader) {
            this.loader = loader;
        }

        void decide(String methodName, InvocationDecision decision) {
            decisions.put(methodName, decision);
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
            return Set.of(BridgeCapability.METHOD_EXTENSION);
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
        public InvocationDecision evaluateMethodExtension(BridgeMethodInvocation invocation) {
            lastInvocation = invocation;
            String methodName = DispatcherRegistry.method(loader, invocation.methodId())
                    .orElseThrow().methodName();
            return decisions.getOrDefault(methodName, InvocationDecision.proceed());
        }
    }
}
