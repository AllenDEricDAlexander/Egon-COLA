package top.egon.cola.component.gateway.provider;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
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
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;
import top.egon.cola.component.gateway.contract.definition.GatewayDefinitionIdentity;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpProviderLeaseRuntimeTest {

    @Test
    void registersActualPortRecoversLostLeaseAndDeregisters() {
        FakeRegistry registry = new FakeRegistry();
        HttpProviderLeaseRuntime runtime = new HttpProviderLeaseRuntime(
                registry,
                serviceKeyFactory(),
                properties()
        );

        runtime.onHttpServerReady(18080);
        assertEquals(HttpProviderRuntimeState.REGISTERED, runtime.state());
        assertEquals(18080, registry.registration.port());
        assertEquals("retail-biz", registry.registration.serviceKey().bizCode());
        assertEquals("orders-app", registry.registration.serviceKey().appCode());

        registry.renewed = false;
        runtime.heartbeatAndRecover();
        assertEquals(2, registry.registrations.get());
        assertEquals(HttpProviderRuntimeState.REGISTERED, runtime.state());

        runtime.close();
        assertEquals(1, registry.deregistrations.get());
    }

    @Test
    void retriesRuntimeRecoveryAfterTransientRegistrationFailure() {
        FakeRegistry registry = new FakeRegistry();
        HttpProviderLeaseRuntime runtime = new HttpProviderLeaseRuntime(
                registry,
                serviceKeyFactory(),
                properties()
        );

        runtime.onHttpServerReady(18080);
        registry.renewed = false;
        registry.registrationFailures.set(2);

        assertDoesNotThrow(runtime::heartbeatAndRecover);
        assertEquals(HttpProviderRuntimeState.RECOVERING, runtime.state());

        runtime.heartbeatAndRecover();
        assertEquals(HttpProviderRuntimeState.RECOVERING, runtime.state());

        runtime.heartbeatAndRecover();
        assertEquals(HttpProviderRuntimeState.REGISTERED, runtime.state());
        assertEquals(4, registry.registrations.get());

        runtime.close();
        assertEquals(1, registry.deregistrations.get());
    }

    @Test
    void mergesDefinitionIdentityAndRejectsProductionLoopback() {
        assertEquals(
                "set-a",
                properties().metadata().get("gateway.definition-set-id")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpProviderRuntimeProperties(
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
                        Map.of(),
                        null
                )
        );
    }

    private HttpProviderRuntimeProperties properties() {
        return new HttpProviderRuntimeProperties(
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
                Map.of("gateway.weight", "100"),
                new GatewayDefinitionIdentity("set-a", "1.0.0", "build-a")
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

    private static final class FakeRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private final AtomicInteger deregistrations = new AtomicInteger();

        private final AtomicInteger registrationFailures = new AtomicInteger();

        private volatile boolean renewed = true;

        private volatile DdcServiceRegistration registration;

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
                String instanceId,
                String leaseId) {
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
}
