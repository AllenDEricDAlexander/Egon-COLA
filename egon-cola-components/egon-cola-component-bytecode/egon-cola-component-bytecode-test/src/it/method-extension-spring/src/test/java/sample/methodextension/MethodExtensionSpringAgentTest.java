package sample.methodextension;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.bytecode.starter.methodextension.MethodExtensionAgentAutoConfiguration;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.event.MethodExtensionEvent;
import top.egon.cola.component.methodextension.event.MethodExtensionEventPublisher;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodExtensionSpringAgentTest {

    @Test
    void coexistsWithJdkAndCglibProxiesWithoutAopOrDuplicateHandlers() {
        List<MethodExtensionEvent> events = new CopyOnWriteArrayList<>();
        DecisionHandler handler = new DecisionHandler();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MethodExtensionAutoConfiguration.class,
                        MethodExtensionAgentAutoConfiguration.class,
                        BytecodeAutoConfiguration.class
                ))
                .withPropertyValues(
                        "egon.cola.component.method-extension.engine=agent")
                .withBean(DecisionHandler.class, () -> handler)
                .withBean(MethodExtensionEventPublisher.class, () -> events::add)
                .run(context -> {
                    assertFalse(context.containsBean("methodExtensionAop"));
                    assertTrue(context.containsBean("methodExtensionRuntimeAdapter"));

                    ProxyFactory jdkFactory = new ProxyFactory(new JdkTarget());
                    jdkFactory.setInterfaces(Contract.class);
                    Contract jdkProxy = (Contract) jdkFactory.getProxy();

                    ProxyFactory cglibFactory = new ProxyFactory(new CglibTarget());
                    cglibFactory.setProxyTargetClass(true);
                    CglibTarget cglibProxy = (CglibTarget) cglibFactory.getProxy();

                    assertEquals("jdk-body", jdkProxy.call("password=secret"));
                    assertEquals("cglib-body", cglibProxy.call("password=secret"));
                    assertEquals(2, handler.calls.get(), events::toString);

                    handler.reject.set(true);
                    assertEquals("agent-rejected", jdkProxy.call("password=secret"));
                    assertEquals("agent-rejected", cglibProxy.call("password=secret"));
                    assertEquals(4, handler.calls.get(), events::toString);
                });

        assertEquals(4, events.size());
        assertTrue(events.stream().noneMatch(event ->
                event.toString().contains("password=secret")));
        System.out.println("METHOD_EXTENSION_SPRING_OK handlers=" + handler.calls.get());
    }

    interface Contract {

        @MethodExtension(handler = DecisionHandler.class)
        String call(String value);
    }

    static final class JdkTarget implements Contract {

        @Override
        public String call(String value) {
            return "jdk-body";
        }
    }

    static class CglibTarget {

        @MethodExtension(handler = DecisionHandler.class)
        public String call(String value) {
            return "cglib-body";
        }
    }

    static final class DecisionHandler implements MethodExtensionHandler {

        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean reject = new AtomicBoolean();

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            calls.incrementAndGet();
            return reject.get()
                    ? MethodExtensionDecision.reject("agent-rejected")
                    : MethodExtensionDecision.allow();
        }
    }
}
