package sample.observation;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.api.observation.ObservationEventSink;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationSpringProxyTest {

    @Test
    void observesTargetsBehindJdkAndCglibProxiesWithoutDoubleCounting() {
        List<ObservationEvent> events = new CopyOnWriteArrayList<>();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BytecodeAutoConfiguration.class))
                .withBean(ObservationEventSink.class, () -> events::add)
                .run(context -> {
                    ProxyFactory jdkFactory = new ProxyFactory(new JdkTarget());
                    jdkFactory.setInterfaces(Contract.class);
                    Contract jdkProxy = (Contract) jdkFactory.getProxy();

                    ProxyFactory cglibFactory = new ProxyFactory(new CglibTarget());
                    cglibFactory.setProxyTargetClass(true);
                    CglibTarget cglibProxy = (CglibTarget) cglibFactory.getProxy();

                    assertEquals("jdk", jdkProxy.call("jdk"));
                    assertEquals("cglib-inner", cglibProxy.call("cglib"));
                });

        assertEquals(1L, count(events, JdkTarget.class, "call"));
        assertEquals(1L, count(events, CglibTarget.class, "call"));
        assertEquals(1L, count(events, CglibTarget.class, "inner"));
        assertTrue(events.stream().noneMatch(event -> event.className().contains("SpringCGLIB")));
        System.out.println("OBSERVATION_SPRING_PROXIES_OK events=" + events.size());
    }

    private long count(List<ObservationEvent> events, Class<?> type, String method) {
        return events.stream()
                .filter(event -> event.className().equals(type.getName()))
                .filter(event -> event.methodName().equals(method))
                .count();
    }

    interface Contract {
        String call(String value);
    }

    static final class JdkTarget implements Contract {

        @Override
        public String call(String value) {
            return value;
        }
    }

    static class CglibTarget {

        public String call(String value) {
            return inner(value);
        }

        private String inner(String value) {
            return value + "-inner";
        }
    }
}
