package top.egon.cola.archetype.source.web.infrastructure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import top.egon.cola.archetype.source.web.infrastructure.aop.DaoMonitorAspect;
import top.egon.cola.archetype.source.web.infrastructure.aop.InfrastructureLogAspect;
import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the split of the former organization log aspect: DAO timings and the client/broker failure
 * boundary must both rethrow the original exception unchanged.
 */
class OrganizationLogAspectTest {

    @Test
    void timesDaoOperationsExactlyOnceAndRethrowsOriginalFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
        try {
            SampleDAO target = new SampleDAO();
            SamplePort proxy = proxy(new DaoMonitorAspect(), target);

            assertEquals("ok", proxy.load());
            assertEquals(1, registry.get("infrastructure.dao").timer().count());

            IllegalStateException failure = new IllegalStateException("boom");
            SampleDAO failing = new SampleDAO(failure);
            Throwable thrown = assertThrows(Throwable.class, () -> proxy(new DaoMonitorAspect(), failing).load());

            assertSame(failure, thrown);
        } finally {
            Metrics.removeRegistry(registry);
        }
    }

    @Test
    void logsAndRethrowsInfrastructureFailure() {
        IllegalStateException failure = new IllegalStateException("boom");
        SamplePort proxy = proxy(new InfrastructureLogAspect(), new SampleClient(failure));

        Throwable thrown = assertThrows(Throwable.class, proxy::load);

        assertSame(failure, thrown);
    }

    private static SamplePort proxy(Object aspect, SamplePort target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    interface SamplePort {
        String load();
    }

    static final class SampleDAO implements SamplePort {
        private final IllegalStateException failure;

        SampleDAO() {
            this(null);
        }

        SampleDAO(IllegalStateException failure) {
            this.failure = failure;
        }

        public String load() {
            if (failure != null) {
                throw failure;
            }
            return "ok";
        }
    }

    static final class SampleClient implements SamplePort {
        private final IllegalStateException failure;

        SampleClient(IllegalStateException failure) {
            this.failure = failure;
        }

        public String load() {
            throw failure;
        }
    }
}
