package top.egon.cola.component.gateway.engine.rpc.service;

import top.egon.cola.component.gateway.engine.rpc.domain.RpcGatewaySlotProperties;
import top.egon.cola.component.gateway.engine.rpc.domain.RpcGatewaySubsystemState;

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
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RpcGatewaySlotRuntimeTest {

    @Test
    void retriesExistingLeaseAfterHeartbeatException() {
        FakeRegistry registry = new FakeRegistry();
        RpcGatewaySlotRuntime runtime = readyRuntime(registry);
        String firstLease = runtime.lease().orElseThrow().leaseId();
        registry.heartbeatFailures.set(1);

        runtime.heartbeatAndRecover();

        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        assertEquals(firstLease, runtime.lease().orElseThrow().leaseId());
        assertTrue(runtime.lastFailure().isPresent());

        runtime.heartbeatAndRecover();

        assertEquals(
                RpcGatewaySubsystemState.REGISTERED_READY,
                runtime.state()
        );
        assertEquals(
                firstLease,
                runtime.lease().orElseThrow().leaseId()
        );
        assertEquals(1, registry.registrations.get());
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
                properties(3, 1),
                serviceTokens().client,
                idpProperties()
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
                properties(3, 1),
                serviceTokens().client,
                idpProperties()
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
                properties(),
                serviceTokens().client,
                idpProperties()
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

    @Test
    void initialServiceTokenFailurePreventsGatewayReady() {
        FakeRegistry registry = new FakeRegistry();
        RecordingServiceTokens serviceTokens = serviceTokens();
        serviceTokens.failures.set(1);
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens.client,
                idpProperties()
        );
        runtime.listenerStarted(19090);

        runtime.engineReady();

        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        assertTrue(runtime.lastFailure().isPresent());
        assertEquals(0, registry.registrations.get());
        runtime.close();
    }

    @Test
    void renewalFailureLeavesReadyAndRetainsLeaseForShutdown() {
        FakeRegistry registry = new FakeRegistry();
        RecordingServiceTokens serviceTokens = serviceTokens();
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens.client,
                idpProperties()
        );
        runtime.listenerStarted(19090);
        runtime.engineReady();
        DdcLeaseSession established = runtime.lease().orElseThrow();
        serviceTokens.failures.set(1);

        runtime.heartbeatAndRecover();

        assertEquals(RpcGatewaySubsystemState.RECOVERING, runtime.state());
        assertEquals(established, runtime.lease().orElseThrow());
        assertEquals(0, registry.heartbeats.get());
        runtime.close();
        assertEquals(1, registry.deregistrations.get());
        assertEquals(2, serviceTokens.calls.get());
    }

    @Test
    void attachesCurrentServiceTokenToRegistrationAndEveryHeartbeat() {
        FakeRegistry registry = new FakeRegistry();
        RecordingServiceTokens serviceTokens = serviceTokens();
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens.client,
                idpProperties()
        );
        runtime.listenerStarted(19090);
        runtime.engineReady();

        runtime.heartbeatAndRecover();

        assertEquals(
                "service-token-1",
                registry.registration.registrationToken()
        );
        assertEquals(
                "service-token-2",
                registry.heartbeatRequest.getRegistrationToken()
        );
        runtime.close();
        assertEquals(2, serviceTokens.calls.get());
    }

    private RpcGatewaySlotRuntime readyRuntime(FakeRegistry registry) {
        RpcGatewaySlotRuntime runtime = new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens().client,
                idpProperties()
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

    private IdpStarterProperties idpProperties() {
        IdpStarterProperties properties = new IdpStarterProperties();
        properties.setResourceUri(java.net.URI.create("https://api.example/ddc"));
        IdpStarterProperties.ServiceClient client =
                new IdpStarterProperties.ServiceClient();
        client.setAppId("ddc-app");
        client.setRegistrationId("ddc-registration");
        properties.setServiceClient(client);
        return properties;
    }

    private RecordingServiceTokens serviceTokens() {
        return new RecordingServiceTokens();
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

        private final AtomicInteger heartbeats = new AtomicInteger();

        private final CountDownLatch secondRegistrationAttempt =
                new CountDownLatch(1);

        private volatile DdcLeaseOperationStatus heartbeatStatus =
                DdcLeaseOperationStatus.RENEWED;

        private volatile DdcServiceRegistration registration;

        private volatile DdcServiceLeaseRequest heartbeatRequest;

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
                DdcServiceLeaseRequest request) {
            heartbeats.incrementAndGet();
            heartbeatRequest = request;
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

    private static final class RecordingServiceTokens {

        private final AtomicInteger calls = new AtomicInteger();

        private final AtomicInteger failures = new AtomicInteger();

        private final IdpServiceOAuth2Client client = mock(
                IdpServiceOAuth2Client.class
        );

        private RecordingServiceTokens() {
            when(client.authorize(any(IdpServiceTokenRequest.class)))
                    .thenAnswer(invocation -> {
                        int sequence = calls.incrementAndGet();
                        if (failures.getAndUpdate(
                                value -> Math.max(0, value - 1)
                        ) > 0) {
                            throw new IllegalStateException(
                                    "IdP service token unavailable"
                            );
                        }
                        Instant issuedAt = Instant.now();
                        return new OAuth2AccessToken(
                                OAuth2AccessToken.TokenType.BEARER,
                                "service-token-" + sequence,
                                issuedAt,
                                issuedAt.plusSeconds(300)
                        );
                    });
        }
    }
}
