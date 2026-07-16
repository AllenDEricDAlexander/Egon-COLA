package sample.accessguard;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardAutoConfiguration;
import top.egon.cola.component.accessguard.event.AccessGuardEvent;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.bytecode.starter.accessguard.AccessGuardAgentAutoConfiguration;
import top.egon.cola.component.bytecode.starter.accessguard.AccessGuardRuntimeAdapter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGuardSpringAgentTest {

    @Test
    void coexistsWithJdkAndCglibProxiesWithoutAopOrDuplicateExecution() {
        List<AccessGuardEvent> events = new CopyOnWriteArrayList<>();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AccessGuardAutoConfiguration.class,
                        AccessGuardAgentAutoConfiguration.class,
                        BytecodeAutoConfiguration.class
                ))
                .withPropertyValues("egon.cola.component.access-guard.engine=agent")
                .withBean(AccessGuardEventPublisher.class, () -> events::add)
                .run(context -> {
                    assertFalse(context.containsBean("accessGuardAop"));
                    assertTrue(context.getBeansOfType(AccessGuardAop.class).isEmpty());
                    assertTrue(context.containsBean("accessGuardRuntimeAdapter"));
                    assertTrue(context.getBean(AccessGuardRuntimeAdapter.class) != null);

                    ProxyFactory jdkFactory = new ProxyFactory(new JdkTarget());
                    jdkFactory.setInterfaces(Contract.class);
                    Contract jdkProxy = (Contract) jdkFactory.getProxy();

                    ProxyFactory cglibFactory = new ProxyFactory(new CglibTarget());
                    cglibFactory.setProxyTargetClass(true);
                    CglibTarget cglibProxy = (CglibTarget) cglibFactory.getProxy();

                    assertEquals("jdk-body", jdkProxy.call("password=secret"));
                    assertEquals("cglib-body", cglibProxy.call("password=secret"));
                    assertEquals(7, new ConstructorTarget(7).value);
                });

        assertEquals(3, events.size(), events::toString);
        assertTrue(events.stream().noneMatch(event ->
                event.toString().contains("password=secret")));
        System.out.println("ACCESS_GUARD_SPRING_OK events=" + events.size());
    }

    interface Contract {

        String call(String value);
    }

    static final class JdkTarget implements Contract {

        @Override
        @AccessGuard(name = "jdk")
        public String call(String value) {
            return "jdk-body";
        }
    }

    static class CglibTarget {

        @AccessGuard(name = "cglib")
        public String call(String value) {
            return "cglib-body";
        }
    }

    static final class ConstructorTarget {

        private final int value;

        @AccessGuard(name = "constructor", key = "value")
        public ConstructorTarget(int value) {
            this.value = value;
        }
    }
}
