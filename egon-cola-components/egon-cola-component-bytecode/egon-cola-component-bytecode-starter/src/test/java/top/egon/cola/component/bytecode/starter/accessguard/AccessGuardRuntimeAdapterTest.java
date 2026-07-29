package top.egon.cola.component.bytecode.starter.accessguard;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardAgentIntegration;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeConstructorInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeFailHint;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.ConstructorGuardDecision;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessGuardRuntimeAdapterTest {

    @Test
    void invokesInstanceAndStaticContinuationsThroughUnifiedEngine() throws Throwable {
        GuardEngine engine = proceedingEngine();
        AccessGuardRuntimeAdapter adapter = adapter(engine);
        adapter.markReady();
        registerMethod(9101L, "instanceValue", "(Ljava/lang/String;)Ljava/lang/String;", 1);
        registerMethod(9102L, "staticValue", "(Ljava/lang/String;)Ljava/lang/String;", 9);

        Object instance = adapter.invokeGuarded(new BridgeGuardedInvocation(
                new GuardedTarget(), GuardedTarget.class, 9101L, new Object[]{"a"},
                MethodHandles.lookup().findVirtual(
                        GuardedTarget.class, "instanceValue",
                        MethodType.methodType(String.class, String.class))));
        Object staticValue = adapter.invokeGuarded(new BridgeGuardedInvocation(
                null, GuardedTarget.class, 9102L, new Object[]{"b"},
                MethodHandles.lookup().findStatic(
                        GuardedTarget.class, "staticValue",
                        MethodType.methodType(String.class, String.class))));

        assertEquals("instance-a", instance);
        assertEquals("static-b", staticValue);
        ArgumentCaptor<GuardInvocation> invocations = ArgumentCaptor.forClass(GuardInvocation.class);
        verify(engine, org.mockito.Mockito.times(2)).execute(invocations.capture());
        assertTrue(invocations.getAllValues().stream().allMatch(
                invocation -> invocation.entryType() == GuardEntryType.AGENT
                        && invocation.kind() == GuardInvocationKind.METHOD));
        assertTrue(adapter instanceof AccessGuardAgentIntegration);
    }

    @Test
    void methodBindingOverridesTypeBinding() throws Throwable {
        GuardEngine engine = proceedingEngine();
        AccessGuardRuntimeAdapter adapter = adapter(engine);
        adapter.markReady();
        registerMethod(9103L, "overridden", "()Ljava/lang/String;", 1);

        adapter.invokeGuarded(new BridgeGuardedInvocation(
                new GuardedTarget(), GuardedTarget.class, 9103L, new Object[0],
                MethodHandles.lookup().findVirtual(
                        GuardedTarget.class, "overridden", MethodType.methodType(String.class))));

        verify(engine).execute(org.mockito.ArgumentMatchers.argThat(
                invocation -> invocation.ruleId().equals("method-rule")));
    }

    @Test
    void constructorUsesTheSameEngineAndIsFailClosedBeforeReady() throws Throwable {
        GuardEngine engine = proceedingEngine();
        AccessGuardRuntimeAdapter adapter = adapter(engine);
        registerConstructor(9110L, "(I)V");
        BridgeConstructorInvocation invocation = new BridgeConstructorInvocation(
                GuardedTarget.class, 9110L, new Object[]{7}, BridgeFailHint.FAIL_CLOSED);

        ConstructorGuardDecision unavailable = adapter.guardConstructor(invocation);
        adapter.markReady();
        ConstructorGuardDecision allowed = adapter.guardConstructor(invocation);

        assertFalse(unavailable.allowed());
        assertTrue(unavailable.throwable() instanceof IllegalStateException);
        assertTrue(allowed.allowed());
        verify(engine).execute(org.mockito.ArgumentMatchers.argThat(
                guard -> guard.entryType() == GuardEntryType.AGENT
                        && guard.kind() == GuardInvocationKind.CONSTRUCTOR
                        && guard.ruleId().equals("constructor-rule")));
    }

    @Test
    void methodAndConstructorPreserveTheSameRejectedOutcome() throws Throwable {
        GuardOutcome outcome = GuardOutcome.rejected(
                "method-rule", GuardDecision.DENY_LIST_HIT, "deny-list", 1L);
        AccessGuardRejectedException rejected = new AccessGuardRejectedException(outcome);
        GuardEngine engine = mock(GuardEngine.class);
        when(engine.execute(any())).thenThrow(rejected);
        AccessGuardRuntimeAdapter adapter = adapter(engine);
        adapter.markReady();
        registerMethod(9120L, "overridden", "()Ljava/lang/String;", 1);
        registerConstructor(9121L, "(I)V");

        AccessGuardRejectedException methodFailure = assertThrows(
                AccessGuardRejectedException.class,
                () -> adapter.invokeGuarded(new BridgeGuardedInvocation(
                        new GuardedTarget(), GuardedTarget.class, 9120L, new Object[0],
                        MethodHandles.lookup().findVirtual(
                                GuardedTarget.class, "overridden", MethodType.methodType(String.class)))));
        ConstructorGuardDecision constructorFailure = adapter.guardConstructor(
                new BridgeConstructorInvocation(
                        GuardedTarget.class, 9121L, new Object[]{1}, BridgeFailHint.FAIL_CLOSED));

        assertSame(outcome, methodFailure.outcome());
        assertFalse(constructorFailure.allowed());
        assertSame(outcome, ((AccessGuardRejectedException) constructorFailure.throwable()).outcome());
    }

    private static GuardEngine proceedingEngine() throws Throwable {
        GuardEngine engine = mock(GuardEngine.class);
        when(engine.execute(any())).thenAnswer(invocation -> {
            GuardInvocation guarded = invocation.getArgument(0);
            return guarded.kind() == GuardInvocationKind.CONSTRUCTOR
                    ? null : guarded.continuation().execute();
        });
        return engine;
    }

    private static AccessGuardRuntimeAdapter adapter(GuardEngine engine) {
        return new AccessGuardRuntimeAdapter(
                engine,
                new MethodMetadataResolver(),
                new GuardBindingResolver());
    }

    private static void registerMethod(long id, String name, String descriptor, int access) {
        register(id, name, descriptor, access, false);
    }

    private static void registerConstructor(long id, String descriptor) {
        register(id, "<init>", descriptor, 1, true);
    }

    private static void register(long id, String name, String descriptor, int access, boolean constructor) {
        DispatcherRegistry.registerMethod(GuardedTarget.class.getClassLoader(), new MethodMetadata(
                id,
                GuardedTarget.class.getName().replace('.', '/'),
                name,
                descriptor,
                access,
                constructor,
                Set.of(BridgeCapability.ACCESS_GUARD)));
    }

    @AccessGuard("type-rule")
    static class GuardedTarget {

        @AccessGuard("constructor-rule")
        GuardedTarget(int value) {
        }

        GuardedTarget() {
        }

        public String instanceValue(String value) {
            return "instance-" + value;
        }

        public static String staticValue(String value) {
            return "static-" + value;
        }

        @AccessGuard("method-rule")
        public String overridden() {
            return "overridden";
        }
    }
}
