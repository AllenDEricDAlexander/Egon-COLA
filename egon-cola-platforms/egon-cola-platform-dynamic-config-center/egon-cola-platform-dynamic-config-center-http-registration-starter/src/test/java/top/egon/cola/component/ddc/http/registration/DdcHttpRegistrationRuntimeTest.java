package top.egon.cola.component.ddc.http.registration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import java.time.Instant;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DdcHttpRegistrationRuntimeTest {

    @Test
    void registersActualPortRecoversLostLeaseAndDeregisters() {
        FakeRegistry registry = new FakeRegistry();
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                admissionTickets()
        );

        runtime.onHttpServerReady(18080);
        assertEquals(DdcHttpRegistrationState.REGISTERED, runtime.state());
        assertEquals(18080, registry.registration.port());
        assertEquals("retail-biz", registry.registration.serviceKey().bizCode());
        assertEquals("orders-app", registry.registration.serviceKey().appCode());

        registry.renewed = false;
        runtime.heartbeatAndRecover();
        assertEquals(DdcHttpRegistrationState.RECOVERING, runtime.state());
        runtime.heartbeatAndRecover();
        assertEquals(2, registry.registrations.get());
        assertEquals(DdcHttpRegistrationState.REGISTERED, runtime.state());

        runtime.close();
        assertEquals(1, registry.deregistrations.get());
    }

    @Test
    void retriesRuntimeRecoveryAfterTransientRegistrationFailure() {
        FakeRegistry registry = new FakeRegistry();
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                admissionTickets()
        );

        runtime.onHttpServerReady(18080);
        registry.renewed = false;
        registry.registrationFailures.set(2);

        assertDoesNotThrow(runtime::heartbeatAndRecover);
        assertEquals(DdcHttpRegistrationState.RECOVERING, runtime.state());

        runtime.heartbeatAndRecover();
        assertEquals(DdcHttpRegistrationState.RECOVERING, runtime.state());

        runtime.heartbeatAndRecover();
        assertEquals(DdcHttpRegistrationState.RECOVERING, runtime.state());

        runtime.heartbeatAndRecover();
        assertEquals(DdcHttpRegistrationState.REGISTERED, runtime.state());
        assertEquals(4, registry.registrations.get());

        runtime.close();
        assertEquals(1, registry.deregistrations.get());
    }

    @Test
    void retainsMetadataAndRejectsProductionLoopback() {
        assertEquals(
                "set-a",
                properties().metadata().get("gateway.definition-set-id")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DdcHttpRegistrationRuntimeProperties(
                        true,
                        "prod",
                        "default",
                        "instance",
                        "orders",
                        "default",
                        "v1",
                        "http",
                        "127.0.0.1",
                        8080,
                        30,
                        10,
                        true,
                        Map.of()
                )
        );
    }

    @Test
    void initialAdmissionFailurePreventsRegistrationAndReady() {
        FakeRegistry registry = new FakeRegistry();
        RecordingAdmissionTickets admissionTickets =
                new RecordingAdmissionTickets();
        admissionTickets.failures.set(1);
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                admissionTickets
        );

        assertThrows(
                IllegalStateException.class,
                () -> runtime.onHttpServerReady(18080)
        );
        assertEquals(DdcHttpRegistrationState.FAILED, runtime.state());
        assertEquals(0, registry.registrations.get());
        runtime.close();
    }

    @Test
    void renewalFailureLeavesReadyAndRetainsLeaseForShutdown() {
        FakeRegistry registry = new FakeRegistry();
        RecordingAdmissionTickets admissionTickets =
                new RecordingAdmissionTickets();
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                admissionTickets
        );
        runtime.onHttpServerReady(18080);
        DdcLeaseSession established = runtime.lease().orElseThrow();
        admissionTickets.failures.set(1);

        runtime.heartbeatAndRecover();

        assertEquals(DdcHttpRegistrationState.RECOVERING, runtime.state());
        assertEquals(established, runtime.lease().orElseThrow());
        assertEquals(0, registry.heartbeats.get());
        runtime.close();
        assertEquals(1, registry.deregistrations.get());
        assertEquals(2, admissionTickets.calls.get());
    }

    @Test
    void attachesCurrentTicketToRegistrationAndEveryHeartbeat() {
        FakeRegistry registry = new FakeRegistry();
        RecordingAdmissionTickets admissionTickets =
                new RecordingAdmissionTickets();
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                admissionTickets
        );

        runtime.onHttpServerReady(18080);
        runtime.heartbeatAndRecover();

        assertEquals(
                "admission-ticket-1",
                registry.registration.admissionTicket()
        );
        assertEquals(
                "admission-ticket-2",
                registry.heartbeatRequest.getAdmissionTicket()
        );
        runtime.close();
        assertEquals(2, admissionTickets.calls.get());
    }

    private DdcHttpRegistrationRuntimeProperties properties() {
        return new DdcHttpRegistrationRuntimeProperties(
                true,
                "test",
                "default",
                "instance",
                "orders",
                "default",
                "v1",
                "http",
                "127.0.0.1",
                0,
                30,
                10,
                true,
                Map.of(
                        "gateway.weight",
                        "100",
                        "gateway.definition-set-id",
                        "set-a"
                )
        );
    }

    private DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail-biz");
        properties.setAppCode("orders-app");
        properties.setEnv("test");
        properties.setNamespace("default");
        return new DdcServiceKeyFactory(properties);
    }

    private DdcAdmissionTicketSupplier admissionTickets() {
        return (bizCode, appCode, environment, instanceId) ->
                new DdcAdmissionTicket(
                        "test-admission-ticket",
                        Instant.parse("2099-01-01T00:00:00Z"),
                        "resource-test",
                        URI.create("urn:egon:resource:test"),
                        1L,
                        bizCode,
                        appCode,
                        environment,
                        instanceId,
                        "kid-test"
                );
    }

    private static final class FakeRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private final AtomicInteger deregistrations = new AtomicInteger();

        private final AtomicInteger heartbeats = new AtomicInteger();

        private final AtomicInteger registrationFailures = new AtomicInteger();

        private volatile boolean renewed = true;

        private volatile DdcServiceRegistration registration;

        private volatile DdcServiceLeaseRequest heartbeatRequest;

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            this.registration = registration;
            int sequence = registrations.incrementAndGet();
            if (registrationFailures.getAndUpdate(
                    value -> Math.max(0, value - 1)
            ) > 0) {
                throw new IllegalStateException("transient registration failure");
            }
            return new DdcLeaseSession(
                    registration.instanceId(),
                    "lease-" + sequence,
                    DdcLeaseRole.HTTP_PROVIDER,
                    30,
                    10,
                    Instant.now(),
                    Instant.now().plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                DdcServiceLeaseRequest request) {
            heartbeats.incrementAndGet();
            heartbeatRequest = request;
            return new DdcLeaseOperationResult(
                    renewed
                            ? DdcLeaseOperationStatus.RENEWED
                            : DdcLeaseOperationStatus.NOT_FOUND,
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

    private static final class RecordingAdmissionTickets
            implements DdcAdmissionTicketSupplier {

        private final AtomicInteger calls = new AtomicInteger();

        private final AtomicInteger failures = new AtomicInteger();

        @Override
        public DdcAdmissionTicket getTicket(
                String bizCode,
                String appCode,
                String environment,
                String instanceId) {
            int sequence = calls.incrementAndGet();
            if (failures.getAndUpdate(value -> Math.max(0, value - 1))
                    > 0) {
                throw new IllegalStateException(
                        "IdP admission unavailable"
                );
            }
            return new DdcAdmissionTicket(
                    "admission-ticket-" + sequence,
                    Instant.parse("2099-01-01T00:00:00Z"),
                    "resource-test",
                    URI.create("urn:egon:resource:test"),
                    1L,
                    bizCode,
                    appCode,
                    environment,
                    instanceId,
                    "kid-test"
            );
        }
    }
}
