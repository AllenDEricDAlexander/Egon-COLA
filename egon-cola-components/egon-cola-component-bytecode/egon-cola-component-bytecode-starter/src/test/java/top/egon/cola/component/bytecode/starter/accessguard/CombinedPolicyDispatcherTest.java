package top.egon.cola.component.bytecode.starter.accessguard;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.accessguard.GuardedInvocationEvaluator;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinedPolicyDispatcherTest {

    @Test
    void unionsCapabilitiesAndRoutesAccessGuardWithoutLosingDelegateFeatures() throws Throwable {
        CombinedPolicyDispatcher dispatcher = new CombinedPolicyDispatcher(
                new DelegateDispatcher(),
                invocation -> "guarded-" + invocation.proceed()
        );
        BridgeGuardedInvocation invocation = new BridgeGuardedInvocation(
                null, CombinedPolicyDispatcherTest.class, 1L, new Object[0],
                MethodHandles.lookup().findStatic(
                        CombinedPolicyDispatcherTest.class,
                        "body",
                        MethodType.methodType(String.class))
        );

        assertEquals(Set.of(BridgeCapability.METHOD_EXTENSION, BridgeCapability.ACCESS_GUARD),
                dispatcher.capabilities());
        assertEquals("guarded-body", dispatcher.invokeGuarded(invocation));
    }

    private static String body() {
        return "body";
    }

    private static final class DelegateDispatcher implements BytecodeRuntimeDispatcher {

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
    }
}
