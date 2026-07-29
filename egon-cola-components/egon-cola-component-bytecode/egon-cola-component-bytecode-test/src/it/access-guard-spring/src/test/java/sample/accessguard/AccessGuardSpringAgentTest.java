package sample.accessguard;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.accessguard.adapter.aop.SpringAopAccessGuardAdvisor;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardAgentIntegration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardCoreAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardLocalStoreAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardObservabilityAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardTimeLimitAutoConfiguration;
import top.egon.cola.component.accessguard.observability.GuardEvent;
import top.egon.cola.component.accessguard.observability.GuardEventListener;
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
        List<GuardEvent> events = new CopyOnWriteArrayList<>();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AccessGuardCoreAutoConfiguration.class,
                        AccessGuardLocalStoreAutoConfiguration.class,
                        AccessGuardTimeLimitAutoConfiguration.class,
                        AccessGuardObservabilityAutoConfiguration.class,
                        AccessGuardAgentAutoConfiguration.class,
                        BytecodeAutoConfiguration.class
                ))
                .withPropertyValues(
                        "egon.cola.component.access-guard.engine=agent",
                        "egon.cola.component.access-guard.key.hmac-secret=integration-secret",
                        "egon.cola.component.access-guard.rules.jdk.key.contributors[0]=GLOBAL",
                        "egon.cola.component.access-guard.rules.cglib.key.contributors[0]=GLOBAL",
                        "egon.cola.component.access-guard.rules.constructor.key.contributors[0]=GLOBAL")
                .withBean(GuardEventListener.class, () -> events::add)
                .run(context -> {
                    assertFalse(context.containsBean("accessGuardAdvisor"));
                    assertTrue(context.getBeansOfType(SpringAopAccessGuardAdvisor.class).isEmpty());
                    assertTrue(context.containsBean("accessGuardRuntimeAdapter"));
                    assertTrue(context.getBean(AccessGuardRuntimeAdapter.class) != null);
                    assertTrue(context.getBean(AccessGuardAgentIntegration.class)
                            instanceof AccessGuardRuntimeAdapter);

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
        @AccessGuard("jdk")
        public String call(String value) {
            return "jdk-body";
        }
    }

    static class CglibTarget {

        @AccessGuard("cglib")
        public String call(String value) {
            return "cglib-body";
        }
    }

    static final class ConstructorTarget {

        private final int value;

        @AccessGuard(value = "constructor", key = "value")
        public ConstructorTarget(int value) {
            this.value = value;
        }
    }
}
