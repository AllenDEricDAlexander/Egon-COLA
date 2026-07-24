package top.egon.cola.component.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
                    "test"
            );
            leases.prepare(methods.providers(), "127.0.0.1", 19090);
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
        return new RpcProviderLifecycle(
                new RpcProviderBeanScanner(
                        context,
                        new RpcContractValidator()
                ),
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                new RpcProviderLeaseManager(
                        registryClient,
                        availability,
                        properties,
                        identity,
                        "test"
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

    private static final class RecordingRegistry
            implements DdcServiceRegistryClient {

        private final List<String> events = new ArrayList<>();

        private DdcServiceRegistration registration;

        private DdcLeaseOperationStatus nextHeartbeat =
                DdcLeaseOperationStatus.RENEWED;

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
            DdcLeaseOperationStatus status = nextHeartbeat;
            nextHeartbeat = DdcLeaseOperationStatus.RENEWED;
            return new DdcLeaseOperationResult(status, Instant.now());
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
