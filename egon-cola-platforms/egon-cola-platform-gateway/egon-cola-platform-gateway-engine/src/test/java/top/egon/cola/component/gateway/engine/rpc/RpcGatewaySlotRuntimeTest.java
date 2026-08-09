package top.egon.cola.component.gateway.engine.rpc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcGatewaySlotRuntimeTest {

    @Test
    void recoversWithNewLeaseAfterHeartbeatException() {
        FakeRegistry registry = new FakeRegistry();
        RpcGatewaySlotRuntime runtime = readyRuntime(registry);
        String firstLease = runtime.lease().orElseThrow().leaseId();
        registry.heartbeatFailures.set(1);

        runtime.heartbeatAndRecover();

        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        assertTrue(runtime.lease().isEmpty());
        assertTrue(runtime.lastFailure().isPresent());

        runtime.heartbeatAndRecover();

        assertEquals(
                RpcGatewaySubsystemState.REGISTERED_READY,
                runtime.state()
        );
        assertNotEquals(
                firstLease,
                runtime.lease().orElseThrow().leaseId()
        );
        assertEquals(2, registry.registrations.get());
        runtime.close();
    }

    @Test
    void keepsRecoveringAcrossRepeatedRegistrationFailures() {
        FakeRegistry registry = new FakeRegistry();
        RpcGatewaySlotRuntime runtime = readyRuntime(registry);
        registry.heartbeatStatus = DdcLeaseOperationStatus.NOT_FOUND;
        runtime.heartbeatAndRecover();
        registry.registrationFailures.set(2);

        runtime.heartbeatAndRecover();
        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        assertTrue(runtime.lastFailure().isPresent());
        runtime.heartbeatAndRecover();
        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        runtime.heartbeatAndRecover();

        assertEquals(
                RpcGatewaySubsystemState.REGISTERED_READY,
                runtime.state()
        );
        assertEquals(4, registry.registrations.get());
        runtime.close();
    }

    @Test
    void closePreventsScheduledRegistrationDuringRecovery()
            throws InterruptedException {
        FakeRegistry registry = new FakeRegistry();
        registry.registrationFailures.set(1);
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties(3, 1)
        );
        runtime.listenerStarted(19090);
        runtime.engineReady();
        int registrationsBeforeClose = registry.registrations.get();

        runtime.close();
        runtime.heartbeatAndRecover();

        assertFalse(registry.secondRegistrationAttempt.await(
                1200,
                TimeUnit.MILLISECONDS
        ));
        assertEquals(RpcGatewaySubsystemState.STOPPED, runtime.state());
        assertEquals(
                registrationsBeforeClose,
                registry.registrations.get()
        );
    }

    @Test
    void initialRegistrationFailureUsesScheduledRecoveryPath()
            throws InterruptedException {
        FakeRegistry registry = new FakeRegistry();
        registry.registrationFailures.set(1);
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties(3, 1)
        );
        runtime.listenerStarted(19090);

        runtime.engineReady();

        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        assertTrue(runtime.lastFailure().isPresent());
        assertTrue(registry.secondRegistrationAttempt.await(
                2,
                TimeUnit.SECONDS
        ));
        assertEquals(2, registry.registrations.get());
        runtime.close();
    }

    @Test
    void registersOnlyAfterEngineReadyAndDeregistersBeforeDrain() {
        FakeRegistry registry = new FakeRegistry();
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
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
        assertEquals("infra", registry.registration.serviceKey().bizCode());
        assertEquals("local", registry.registration.serviceKey().env());
        assertEquals("ge", registry.registration.serviceKey().appCode());
        assertEquals(
                "grpc",
                registry.registration.metadata().get("egon.rpc.transport")
        );
        assertEquals(
                "engine",
                registry.registration.metadata().get("gateway.component")
        );

        runtime.beginDrain();
        assertEquals(RpcGatewaySubsystemState.DRAINING, runtime.state());
        assertEquals(1, registry.deregistrations.get());
        assertTrue(runtime.lease().isEmpty());
        runtime.close();
    }

    private RpcGatewaySlotRuntime readyRuntime(FakeRegistry registry) {
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties()
        );
        runtime.listenerStarted(19090);
        runtime.engineReady();
        return runtime;
    }

    private RpcGatewaySlotProperties properties() {
        return properties(30, 10);
    }

    private DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("infra");
        properties.setAppCode("ge");
        properties.setEnv("local");
        properties.setNamespace("default");
        return new DdcServiceKeyFactory(properties);
    }

    private RpcGatewaySlotProperties properties(
            int leaseSeconds,
            int heartbeatIntervalSeconds) {
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
                leaseSeconds,
                heartbeatIntervalSeconds
        );
    }

    private static final class FakeRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private final AtomicInteger deregistrations = new AtomicInteger();

        private final AtomicInteger registrationFailures =
                new AtomicInteger();

        private final AtomicInteger heartbeatFailures = new AtomicInteger();

        private final CountDownLatch secondRegistrationAttempt =
                new CountDownLatch(1);

        private volatile DdcLeaseOperationStatus heartbeatStatus =
                DdcLeaseOperationStatus.RENEWED;

        private volatile DdcServiceRegistration registration;

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            this.registration = registration;
            int sequence = registrations.incrementAndGet();
            if (sequence == 2) {
                secondRegistrationAttempt.countDown();
            }
            if (registrationFailures.getAndUpdate(
                    failures -> Math.max(0, failures - 1)
            ) > 0) {
                throw new IllegalStateException("registration failed");
            }
            return new DdcLeaseSession(
                    registration.instanceId(),
                    "lease-" + sequence,
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
            if (heartbeatFailures.getAndUpdate(
                    failures -> Math.max(0, failures - 1)
            ) > 0) {
                throw new IllegalStateException("heartbeat failed");
            }
            return new DdcLeaseOperationResult(
                    heartbeatStatus,
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
