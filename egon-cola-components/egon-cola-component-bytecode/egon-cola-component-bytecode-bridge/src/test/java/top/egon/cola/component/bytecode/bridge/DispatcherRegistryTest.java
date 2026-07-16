package top.egon.cola.component.bytecode.bridge;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatcherRegistryTest {

    @Test
    void rejectsDuplicateLiveDispatcherAndAllowsRegistrationAfterClose() {
        ClassLoader loader = new ClassLoader() {
        };
        TestDispatcher first = new TestDispatcher(1, 0, Set.of(BridgeCapability.EXECUTOR));
        TestDispatcher second = new TestDispatcher(1, 0, Set.of(BridgeCapability.EXECUTOR));

        DispatcherRegistration registration = DispatcherRegistry.register(loader, "runtime-1", first);
        assertThrows(IllegalStateException.class,
                () -> DispatcherRegistry.register(loader, "runtime-2", second));
        registration.close();
        registration.close();

        try (DispatcherRegistration ignored = DispatcherRegistry.register(loader, "runtime-2", second)) {
            assertTrue(DispatcherRegistry.status(loader).registered());
        }
        assertFalse(DispatcherRegistry.status(loader).registered());
    }

    @Test
    void rejectsMajorMismatchAndNegotiatesCapabilitiesAcrossMinorVersions() {
        ClassLoader loader = new ClassLoader() {
        };
        assertThrows(IllegalArgumentException.class, () -> DispatcherRegistry.register(
                loader, "runtime", new TestDispatcher(2, 0, Set.of(BridgeCapability.EXECUTOR))));

        TestDispatcher newerMinor = new TestDispatcher(
                1, 3, Set.of(BridgeCapability.EXECUTOR, BridgeCapability.OBSERVATION));
        try (DispatcherRegistration ignored = DispatcherRegistry.register(
                loader, "runtime", newerMinor)) {
            BridgeStatus status = DispatcherRegistry.status(loader);
            assertEquals(3, status.protocolMinor());
            assertEquals(Set.of(BridgeCapability.EXECUTOR, BridgeCapability.OBSERVATION),
                    status.capabilities());
        }
    }

    @Test
    void registryDoesNotKeepApplicationLoaderAlive() {
        WeakReference<ClassLoader> reference = registerTemporaryLoader();
        for (int attempt = 0; attempt < 100 && reference.get() != null; attempt++) {
            System.gc();
            byte[] pressure = new byte[64 * 1024];
            pressure[0] = 1;
            Thread.yield();
        }
        assertNull(reference.get(), "registry retained the application ClassLoader");
    }

    private WeakReference<ClassLoader> registerTemporaryLoader() {
        ClassLoader loader = new ClassLoader() {
        };
        DispatcherRegistry.register(loader, "runtime",
                new TestDispatcher(1, 0, Set.of(BridgeCapability.EXECUTOR)));
        return new WeakReference<>(loader);
    }

    private record TestDispatcher(
            int protocolMajor,
            int protocolMinor,
            Set<BridgeCapability> capabilities
    ) implements BytecodeRuntimeDispatcher {

        @Override
        public Runnable decorateRunnable(
                Class<?> callerClass,
                java.util.concurrent.Executor executor,
                Runnable task,
                long callSiteId
        ) {
            return task;
        }

        @Override
        public <V> java.util.concurrent.Callable<V> decorateCallable(
                Class<?> callerClass,
                java.util.concurrent.Executor executor,
                java.util.concurrent.Callable<V> task,
                long callSiteId
        ) {
            return task;
        }
    }
}
