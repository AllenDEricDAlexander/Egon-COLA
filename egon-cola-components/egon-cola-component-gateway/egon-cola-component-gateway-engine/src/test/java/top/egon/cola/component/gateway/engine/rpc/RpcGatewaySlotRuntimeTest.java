package top.egon.cola.component.gateway.engine.rpc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcGatewaySlotRuntimeTest {

    @Test
    void registersOnlyAfterEngineReadyAndDeregistersBeforeDrain() {
        FakeRegistry registry = new FakeRegistry();
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                properties()
        );

        runtime.listenerStarted(19090);
        assertEquals(
                RpcGatewaySubsystemState.LISTENING_NOT_REGISTERED,
                runtime.state()
        );
        assertEquals(0, registry.registrations.get());

        runtime.engineReady();
        assertEquals(
                RpcGatewaySubsystemState.REGISTERED_READY,
                runtime.state()
        );
        assertEquals(1, registry.registrations.get());
        assertEquals(
                "grpc",
                registry.registration.metadata().get("egon.rpc.transport")
        );

        runtime.beginDrain();
        assertEquals(RpcGatewaySubsystemState.DRAINING, runtime.state());
        assertEquals(1, registry.deregistrations.get());
        assertTrue(runtime.lease().isEmpty());
        runtime.close();
    }

    private RpcGatewaySlotProperties properties() {
        return new RpcGatewaySlotProperties(
                true,
                "test",
                "default",
                "engine-a",
                "127.0.0.1",
                "gateway-internal",
                "default",
                "v1",
                "group-a",
                "5.2.3",
                "5.2.3",
                30,
                10
        );
    }

    private static final class FakeRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private final AtomicInteger deregistrations = new AtomicInteger();

        private volatile DdcServiceRegistration registration;

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            this.registration = registration;
            registrations.incrementAndGet();
            return new DdcLeaseSession(
                    registration.instanceId(),
                    "lease",
                    DdcLeaseRole.INTERNAL_GATEWAY,
                    30,
                    10,
                    Instant.now(),
                    Instant.now().plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.now().plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            deregistrations.incrementAndGet();
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.DELETED,
                    Instant.now()
            );
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            throw new UnsupportedOperationException();
        }
    }
}
