package top.egon.cola.component.ddc.http.registration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
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
import top.egon.cola.platform.idp.contract.ServiceTokenContext;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcHttpRegistrationRuntimeTest {

    @Test
    void registersActualPortRecoversLostLeaseAndDeregisters() {
        FakeRegistry registry = new FakeRegistry();
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens().client,
                idpProperties()
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
                serviceTokens().client,
                idpProperties()
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
    void initialServiceTokenFailurePreventsRegistrationAndReady() {
        FakeRegistry registry = new FakeRegistry();
        RecordingServiceTokens serviceTokens = serviceTokens();
        serviceTokens.failures.set(1);
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens.client,
                idpProperties()
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
        RecordingServiceTokens serviceTokens = serviceTokens();
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens.client,
                idpProperties()
        );
        runtime.onHttpServerReady(18080);
        DdcLeaseSession established = runtime.lease().orElseThrow();
        serviceTokens.failures.set(1);

        runtime.heartbeatAndRecover();

        assertEquals(DdcHttpRegistrationState.RECOVERING, runtime.state());
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
        DdcHttpRegistrationRuntime runtime = new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory(),
                properties(),
                serviceTokens.client,
                idpProperties()
        );

        runtime.onHttpServerReady(18080);
        runtime.heartbeatAndRecover();

        assertEquals(
                "service-token-1",
                registry.registration.registrationToken()
        );
        assertEquals(
                "service-token-2",
                registry.heartbeatRequest.getRegistrationToken()
        );
        assertEquals(2, serviceTokens.requests.size());
        IdpServiceTokenRequest request = serviceTokens.requests.getFirst();
        assertEquals(
                java.net.URI.create("https://api.example/ddc"),
                request.audience()
        );
        assertEquals(ServiceTokenContext.PLATFORM, request.context());
        assertEquals(null, request.tenantId());
        assertEquals(Set.of("ddc:registration:write"), request.scopes());
        runtime.close();
        assertEquals(2, serviceTokens.calls.get());
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

    private static final class RecordingServiceTokens {

        private final AtomicInteger calls = new AtomicInteger();

        private final AtomicInteger failures = new AtomicInteger();

        private final List<IdpServiceTokenRequest> requests =
                new ArrayList<>();

        private final IdpServiceOAuth2Client client = mock(
                IdpServiceOAuth2Client.class
        );

        private RecordingServiceTokens() {
            when(client.authorize(any(IdpServiceTokenRequest.class)))
                    .thenAnswer(invocation -> {
                        requests.add(invocation.getArgument(0));
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
