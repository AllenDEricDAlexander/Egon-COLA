package top.egon.cola.component.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
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
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderLifecycleTest {

    @Test
    void shouldBindThenRegisterRandomPortAndDeregisterBeforeStop() {
        RecordingRegistry registryClient = new RecordingRegistry();
        try (AnnotationConfigApplicationContext context = providerContext()) {
            RpcProviderLifecycle lifecycle = lifecycle(
                    context,
                    registryClient,
                    configuredProperties()
            );

            lifecycle.start();

            assertThat(lifecycle.boundPort()).isPositive();
            assertThat(registryClient.registration.port())
                    .isEqualTo(lifecycle.boundPort());
            assertThat(registryClient.registration.metadata())
                    .containsEntry("egon.rpc.transport", "grpc")
                    .containsEntry("egon.rpc.serialization", "protobuf")
                    .containsEntry("egon.rpc.runtime-version", "test");
            assertThat(registryClient.registration.serviceKey().bizCode())
                    .isEqualTo("retail-biz");
            assertThat(registryClient.registration.serviceKey().appCode())
                    .isEqualTo("orders-app");
            assertThat(registryClient.registration.serviceKey().env())
                    .isEqualTo("test");
            assertThat(registryClient.registration.instanceId())
                    .isEqualTo("provider-process");

            lifecycle.stop();

            assertThat(registryClient.events)
                    .containsExactly("register", "deregister");
        }
    }

    @Test
    void shouldFailFastForUnroutableAdvertisedHost() {
        EgonRpcProperties properties = configuredProperties();
        properties.getProvider().setAdvertisedHost("0.0.0.0");
        try (AnnotationConfigApplicationContext context = providerContext()) {
            RpcProviderLifecycle lifecycle = lifecycle(
                    context,
                    new RecordingRegistry(),
                    properties
            );

            assertThatThrownBy(lifecycle::start)
                    .isInstanceOf(EgonRpcException.class);
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }

    @Test
    void shouldFailWithExistingErrorSemanticsWhenRegistryHasNoProvider() {
        RecordingRegistry registryClient = new RecordingRegistry();
        EgonRpcProperties properties = configuredProperties();
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProcessIdentity identity = processIdentity();
        RpcProviderLifecycle lifecycle = new RpcProviderLifecycle(
                new RpcProviderMethodRegistry(List.of()),
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                new RpcProviderLeaseManager(
                        registryClient,
                        availability,
                        properties,
                        identity,
                        "test",
                        serviceKeyFactory()
                ),
                availability,
                new RpcProviderServerInterceptor(),
                properties,
                identity
        );

        assertThatThrownBy(lifecycle::start)
                .isInstanceOf(EgonRpcException.class)
                .hasMessageContaining("no RPC Provider bean");
        assertThat(registryClient.events).isEmpty();
    }

    @Test
    void contributorFailureStopsProviderWithoutRegisteringAnyLease() {
        RecordingRegistry registryClient = new RecordingRegistry();
        try (AnnotationConfigApplicationContext context = providerContext()) {
            EgonRpcProperties properties = configuredProperties();
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            RpcProcessIdentity identity = processIdentity();
            RpcProviderMethodRegistry registry =
                    new RpcProviderBeanScanner(
                            context,
                            new RpcContractValidator()
                    ).scan();
            RpcProviderMetadataMerger metadataMerger =
                    new RpcProviderMetadataMerger(List.of(service -> {
                        throw new IllegalStateException("contributor failed");
                    }));
            RpcProviderLifecycle lifecycle = new RpcProviderLifecycle(
                    registry,
                    new RpcServerServiceDefinitionFactory(availability),
                    new RpcProviderServerFactory(),
                    new RpcProviderLeaseManager(
                            registryClient,
                            availability,
                            properties,
                            identity,
                            "test",
                            metadataMerger,
                            serviceKeyFactory()
                    ),
                    availability,
                    new RpcProviderServerInterceptor(),
                    properties,
                    identity
            );

            assertThatThrownBy(lifecycle::start)
                    .isInstanceOf(EgonRpcException.class)
                    .hasRootCauseMessage("contributor failed");
            assertThat(registryClient.events).isEmpty();
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }

    @Test
    void mergedMetadataStillUsesDdcCapacityGuardBeforeLeaseRegistration() {
        RecordingRegistry registryClient = new RecordingRegistry();
        EgonRpcProperties properties = configuredProperties();
        Map<String, String> metadata = new LinkedHashMap<>();
        for (int index = 0; index < 30; index++) {
            metadata.put("custom.key." + index, "value");
        }
        properties.getProvider().setMetadata(metadata);
        try (AnnotationConfigApplicationContext context = providerContext()) {
            RpcProviderLifecycle lifecycle = lifecycle(
                    context,
                    registryClient,
                    properties
            );

            assertThatThrownBy(lifecycle::start)
                    .isInstanceOf(EgonRpcException.class)
                    .hasRootCauseMessage(
                            "metadata must contain at most 32 non-reserved entries"
                    );
            assertThat(registryClient.events).isEmpty();
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }

    @Test
    void shouldReplaceLostLeaseBeforeRestoringAvailability() {
        RecordingRegistry registryClient = new RecordingRegistry();
        try (AnnotationConfigApplicationContext context = providerContext()) {
            EgonRpcProperties properties = configuredProperties();
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            RpcProcessIdentity identity = processIdentity();
            RpcProviderMethodRegistry methods = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            ).scan();
            RpcProviderLeaseManager leases = new RpcProviderLeaseManager(
                    registryClient,
                    availability,
                    properties,
                    identity,
                    "test",
                    serviceKeyFactory()
            );
            leases.prepare(methods.providers(), "127.0.0.1", 19090);
            leases.enableRecovery();
            leases.registerAll();
            String firstLease = leases.currentLeases().values()
                    .iterator()
                    .next()
                    .leaseId();
            registryClient.nextHeartbeat =
                    DdcLeaseOperationStatus.LEASE_MISMATCH;

            leases.heartbeatAndRecover();

            String secondLease = leases.currentLeases().values()
                    .iterator()
                    .next()
                    .leaseId();
            assertThat(secondLease).isNotEqualTo(firstLease);
            assertThat(registryClient.events)
                    .containsExactly("register", "heartbeat", "register");
        }
    }

    @Test
    void shouldDisableRecoveryBeforeDeregisteringDuringHeartbeat() {
        RecordingRegistry registryClient = new RecordingRegistry();
        registryClient.blockHeartbeat = true;
        try (AnnotationConfigApplicationContext context = providerContext()) {
            EgonRpcProperties properties = configuredProperties();
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            RpcProviderMethodRegistry methods = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            ).scan();
            RpcProviderLeaseManager leases = new RpcProviderLeaseManager(
                    registryClient,
                    availability,
                    properties,
                    processIdentity(),
                    "test",
                    serviceKeyFactory()
            );
            leases.prepare(methods.providers(), "127.0.0.1", 19090);
            leases.enableRecovery();
            leases.registerAll();
            registryClient.nextHeartbeat =
                    DdcLeaseOperationStatus.LEASE_MISMATCH;

            CompletableFuture<Void> heartbeat = CompletableFuture.runAsync(
                    leases::heartbeatAndRecover
            );
            registryClient.awaitHeartbeat();
            CompletableFuture<Void> disable = CompletableFuture.runAsync(
                    leases::disableRecovery
            );

            assertThat(disable).isNotDone();
            registryClient.releaseHeartbeat();
            heartbeat.join();
            disable.join();
            leases.deregisterAll();
            leases.heartbeatAndRecover();

            assertThat(registryClient.events)
                    .containsExactly(
                            "register",
                            "heartbeat",
                            "register",
                            "deregister"
                    );
        }
    }

    private AnnotationConfigApplicationContext providerContext() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.registerBean(
                RpcProviderTestFixtures.EchoProvider.class
        );
        context.refresh();
        return context;
    }

    private RpcProviderLifecycle lifecycle(
            AnnotationConfigApplicationContext context,
            RecordingRegistry registryClient,
            EgonRpcProperties properties) {
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProcessIdentity identity = processIdentity();
        RpcProviderMethodRegistry registry = new RpcProviderBeanScanner(
                context,
                new RpcContractValidator()
        ).scan();
        return new RpcProviderLifecycle(
                registry,
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                new RpcProviderLeaseManager(
                        registryClient,
                        availability,
                        properties,
                        identity,
                        "test",
                        serviceKeyFactory()
                ),
                availability,
                new RpcProviderServerInterceptor(),
                properties,
                identity
        );
    }

    private EgonRpcProperties configuredProperties() {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getProvider().setEnabled(true);
        properties.getProvider().setBindAddress("127.0.0.1");
        properties.getProvider().setPort(0);
        properties.getProvider().setAdvertisedHost("127.0.0.1");
        properties.getProvider().setGracefulShutdownTimeoutMs(100);
        return properties;
    }

    private RpcProcessIdentity processIdentity() {
        return new RpcProcessIdentity(
                "provider-test",
                "test",
                "default",
                "127.0.0.1",
                1,
                "provider-process"
        );
    }

    private DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail-biz");
        properties.setAppCode("orders-app");
        properties.setEnv("factory-default");
        properties.setNamespace("default");
        return new DdcServiceKeyFactory(properties);
    }

    private static final class RecordingRegistry
            implements DdcServiceRegistryClient {

        private final List<String> events = new ArrayList<>();

        private DdcServiceRegistration registration;

        private DdcLeaseOperationStatus nextHeartbeat =
                DdcLeaseOperationStatus.RENEWED;

        private boolean blockHeartbeat;

        private final CountDownLatch heartbeatStarted = new CountDownLatch(1);

        private final CountDownLatch heartbeatReleased = new CountDownLatch(1);

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            events.add("register");
            this.registration = registration;
            Instant now = Instant.now();
            return new DdcLeaseSession(
                    registration.instanceId(),
                    UUID.randomUUID().toString(),
                    registration.serviceKey().serviceKind().leaseRole(),
                    registration.leaseSeconds(),
                    registration.heartbeatIntervalSeconds(),
                    now,
                    now.plusSeconds(registration.leaseSeconds())
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            events.add("heartbeat");
            heartbeatStarted.countDown();
            if (blockHeartbeat) {
                try {
                    if (!heartbeatReleased.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "heartbeat test barrier timed out"
                        );
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "heartbeat test barrier interrupted",
                            exception
                    );
                }
            }
            DdcLeaseOperationStatus status = nextHeartbeat;
            nextHeartbeat = DdcLeaseOperationStatus.RENEWED;
            return new DdcLeaseOperationResult(status, Instant.now());
        }

        private void awaitHeartbeat() {
            try {
                assertThat(heartbeatStarted.await(2, TimeUnit.SECONDS))
                        .isTrue();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }

        private void releaseHeartbeat() {
            heartbeatReleased.countDown();
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            events.add("deregister");
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
