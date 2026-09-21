package top.egon.cola.archetype.source.lightopen.infrastructure.aop;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InfrastructureAspectTest {
    @Test
    void records_dao_timing() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleDAO());
        factory.addAspect(new DaoMonitorAspect(registry));
        SamplePort proxy = factory.getProxy();

        assertEquals("ok", proxy.load());
        assertEquals(1, registry.get("infrastructure.dao").timer().count());
    }

    @Test
    void logs_and_rethrows_infrastructure_failure() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new FailingClient());
        factory.addAspect(new InfrastructureLogAspect());
        SamplePort proxy = factory.getProxy();

        assertThrows(IllegalStateException.class, proxy::load);
    }

    interface SamplePort { String load(); }
    static final class SampleDAO implements SamplePort {
        public String load() { return "ok"; }
    }
    static final class FailingClient implements SamplePort {
        public String load() { throw new IllegalStateException("failed"); }
    }
}
