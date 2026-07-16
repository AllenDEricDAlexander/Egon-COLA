package top.egon.cola.component.bytecode.starter.methodextension;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeMethodInvocation;
import top.egon.cola.component.bytecode.bridge.DecisionKind;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.InvocationDecision;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionNotReadyPolicy;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.event.NoopMethodExtensionEventPublisher;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodExtensionRuntimeAdapterTest {

    @Test
    void mapsNotReadyPoliciesExactly() {
        BridgeMethodInvocation invocation = new BridgeMethodInvocation(
                new Object(), Object.class, 1L, new Object[0]);

        assertEquals(DecisionKind.PROCEED,
                notReady(MethodExtensionNotReadyPolicy.PROCEED)
                        .evaluateMethodExtension(invocation).kind());
        assertEquals(DecisionKind.RETURN_NULL,
                notReady(MethodExtensionNotReadyPolicy.REJECT)
                        .evaluateMethodExtension(invocation).kind());
        InvocationDecision failure = notReady(MethodExtensionNotReadyPolicy.FAIL)
                .evaluateMethodExtension(invocation);
        assertEquals(DecisionKind.THROW, failure.kind());
        assertTrue(failure.throwable() instanceof IllegalStateException);
    }

    @Test
    void resolvesPrivateAndInterfaceMethodsAndInvokesHandlerExactlyOnce() {
        RecordingHandler handler = new RecordingHandler();
        MethodExtensionRuntimeAdapter adapter = readyAdapter(handler);
        register(8101L, "interfaceValue", "(Ljava/lang/String;)Ljava/lang/String;",
                TestService.class, java.lang.reflect.Modifier.PUBLIC);
        register(8102L, "privateValue", "()Ljava/lang/String;",
                TestService.class, java.lang.reflect.Modifier.PRIVATE);
        TestService target = new TestService();

        handler.decision = MethodExtensionDecision.reject("interface-short");
        InvocationDecision interfaceDecision = adapter.evaluateMethodExtension(
                new BridgeMethodInvocation(
                        target, TestService.class, 8101L, new Object[]{"argument"}));
        assertEquals(DecisionKind.RETURN_VALUE, interfaceDecision.kind());
        assertEquals("interface-short", interfaceDecision.value());

        handler.decision = MethodExtensionDecision.reject("private-short");
        InvocationDecision privateDecision = adapter.evaluateMethodExtension(
                new BridgeMethodInvocation(target, TestService.class, 8102L, new Object[0]));
        assertEquals(DecisionKind.RETURN_VALUE, privateDecision.kind());
        assertEquals("private-short", privateDecision.value());
        Method privateMethod = new MethodMetadataResolver().resolve(TestService.class, 8102L);
        assertEquals("privateValue", privateMethod.getName());
        assertTrue(privateMethod.trySetAccessible());
        assertEquals(2, handler.calls);
        assertNotNull(handler.lastContext.target());
        assertNotNull(handler.lastContext.method());
    }

    @Test
    void mapsProceedNullValueAndExactHandlerFailure() {
        RecordingHandler handler = new RecordingHandler();
        MethodExtensionRuntimeAdapter adapter = readyAdapter(handler);
        register(8103L, "interfaceValue", "(Ljava/lang/String;)Ljava/lang/String;",
                TestService.class, java.lang.reflect.Modifier.PUBLIC);
        register(8104L, "voidValue", "()V",
                TestService.class, java.lang.reflect.Modifier.PRIVATE);
        TestService target = new TestService();

        handler.decision = MethodExtensionDecision.allow();
        assertEquals(DecisionKind.PROCEED, adapter.evaluateMethodExtension(
                new BridgeMethodInvocation(
                        target, TestService.class, 8103L, new Object[]{"argument"})).kind());

        handler.decision = MethodExtensionDecision.reject();
        assertEquals(DecisionKind.RETURN_NULL, adapter.evaluateMethodExtension(
                new BridgeMethodInvocation(
                        target, TestService.class, 8104L, new Object[0])).kind());

        RuntimeException failure = new RuntimeException("same-instance");
        handler.failure = failure;
        InvocationDecision failed = adapter.evaluateMethodExtension(
                new BridgeMethodInvocation(
                        target, TestService.class, 8103L, new Object[]{"argument"}));
        assertEquals(DecisionKind.THROW, failed.kind());
        assertSame(failure, failed.throwable());
    }

    private MethodExtensionRuntimeAdapter notReady(MethodExtensionNotReadyPolicy policy) {
        return new MethodExtensionRuntimeAdapter(
                () -> null, new MethodMetadataResolver(), policy);
    }

    private MethodExtensionRuntimeAdapter readyAdapter(RecordingHandler handler) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("recordingHandler", handler);
        MethodExtensionExecutionService service = new MethodExtensionExecutionService(
                new MethodExtensionMethodResolver(),
                new MethodExtensionHandlerResolver(beanFactory),
                new MethodExtensionResponseResolver(
                        beanFactory.getBeanProvider(com.fasterxml.jackson.databind.ObjectMapper.class)),
                new NoopMethodExtensionEventPublisher()
        );
        MethodExtensionRuntimeAdapter adapter = new MethodExtensionRuntimeAdapter(
                () -> service,
                new MethodMetadataResolver(),
                MethodExtensionNotReadyPolicy.PROCEED
        );
        adapter.markReady();
        return adapter;
    }

    private void register(
            long methodId,
            String methodName,
            String descriptor,
            Class<?> owner,
            int access
    ) {
        DispatcherRegistry.registerMethod(owner.getClassLoader(), new MethodMetadata(
                methodId,
                owner.getName().replace('.', '/'),
                methodName,
                descriptor,
                access,
                false,
                Set.of(BridgeCapability.METHOD_EXTENSION)
        ));
    }

    interface TestContract {

        @MethodExtension(handler = RecordingHandler.class)
        String interfaceValue(String value);
    }

    static class TestService implements TestContract {

        @Override
        public String interfaceValue(String value) {
            return value;
        }

        @MethodExtension(handler = RecordingHandler.class)
        private String privateValue() {
            return "body";
        }

        @MethodExtension(handler = RecordingHandler.class)
        private void voidValue() {
        }
    }

    static class RecordingHandler implements MethodExtensionHandler {

        private int calls;
        private MethodExtensionDecision decision = MethodExtensionDecision.allow();
        private RuntimeException failure;
        private MethodExtensionContext lastContext;

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            calls++;
            lastContext = context;
            if (failure != null) {
                throw failure;
            }
            return decision;
        }
    }
}
