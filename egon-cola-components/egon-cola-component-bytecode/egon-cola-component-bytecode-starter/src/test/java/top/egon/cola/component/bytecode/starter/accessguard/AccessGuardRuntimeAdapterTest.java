package top.egon.cola.component.bytecode.starter.accessguard;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.AccessGuardFailureHandler;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessGuardRuntimeAdapterTest {

    @Test
    void invokesInstanceAndStaticContinuationsThroughAgentJoinPoint() throws Throwable {
        AccessGuardExecutionService service = mock(AccessGuardExecutionService.class);
        when(service.execute(any())).thenAnswer(invocation ->
                invocation.<org.aspectj.lang.ProceedingJoinPoint>getArgument(0).proceed());
        AccessGuardRuleResolver ruleResolver = mock(AccessGuardRuleResolver.class);
        when(ruleResolver.resolve(any(java.lang.reflect.Method.class))).thenReturn(rule());
        AccessGuardRuntimeAdapter adapter = adapter(service, ruleResolver, FailStrategy.FAIL_OPEN);
        adapter.markReady();
        register(9101L, "instanceValue", "(Ljava/lang/String;)Ljava/lang/String;", 1);
        register(9102L, "staticValue", "(Ljava/lang/String;)Ljava/lang/String;", 9);

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
    }

    @Test
    void appliesGlobalFailStrategyBeforeRuntimeIsReady() throws Throwable {
        BridgeGuardedInvocation invocation = new BridgeGuardedInvocation(
                null, GuardedTarget.class, 9199L, new Object[]{"open"},
                MethodHandles.lookup().findStatic(
                        GuardedTarget.class, "staticValue",
                        MethodType.methodType(String.class, String.class)));
        assertEquals("static-open", adapter(null, mock(AccessGuardRuleResolver.class),
                FailStrategy.FAIL_OPEN).invokeGuarded(invocation));
        assertThrows(RuntimeException.class, () -> adapter(
                null, mock(AccessGuardRuleResolver.class), FailStrategy.FAIL_CLOSED)
                .invokeGuarded(invocation));
    }

    private AccessGuardRuntimeAdapter adapter(
            AccessGuardExecutionService service,
            AccessGuardRuleResolver ruleResolver,
            FailStrategy failStrategy
    ) {
        AccessGuardProperties properties = new AccessGuardProperties();
        properties.setFailStrategy(failStrategy);
        return new AccessGuardRuntimeAdapter(
                () -> service,
                new MethodMetadataResolver(),
                ruleResolver,
                new AccessGuardFailureHandler(properties)
        );
    }

    private void register(long id, String name, String descriptor, int access) {
        DispatcherRegistry.registerMethod(GuardedTarget.class.getClassLoader(), new MethodMetadata(
                id,
                GuardedTarget.class.getName().replace('.', '/'),
                name,
                descriptor,
                access,
                false,
                Set.of(BridgeCapability.ACCESS_GUARD)
        ));
    }

    private AccessGuardRule rule() {
        return new AccessGuardRule(
                "guard", "all", "", false, List.of(), WhiteListMode.GATEKEEPER,
                false, 1, 1, TimeUnit.SECONDS, false, 0, Duration.ZERO, false,
                false, Duration.ZERO, TimeoutExecutorType.THREAD_POOL, false, true,
                "", "", FailStrategy.FAIL_OPEN
        );
    }

    static class GuardedTarget {

        public String instanceValue(String value) {
            return "instance-" + value;
        }

        public static String staticValue(String value) {
            return "static-" + value;
        }
    }
}
