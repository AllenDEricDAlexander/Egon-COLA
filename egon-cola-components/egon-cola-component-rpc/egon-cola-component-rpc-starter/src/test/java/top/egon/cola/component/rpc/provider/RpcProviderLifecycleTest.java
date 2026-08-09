package top.egon.cola.component.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderLifecycleTest {

    @Test
    void bindsRegistersAndDeregistersThroughTheNeutralRegistry() {
        RecordingRegistry registry = new RecordingRegistry();
        try (AnnotationConfigApplicationContext context = providerContext()) {
            RpcProviderLifecycle lifecycle = lifecycle(
                    context,
                    registry,
                    configuredProperties()
            );

            lifecycle.start();

            assertThat(lifecycle.boundPort()).isPositive();
            assertThat(registry.registration.port())
                    .isEqualTo(lifecycle.boundPort());
            assertThat(registry.registration.serviceIdentity())
                    .isEqualTo(new RpcServiceIdentity(
                            "egon.rpc.fixture.v1.UnaryFixtureService",
                            "test",
                            "1.0.0"
                    ));
            assertThat(registry.registration.processIdentity().env())
                    .isEqualTo("test");
            assertThat(registry.registration.processIdentity().instanceId())
                    .isEqualTo("provider-process");
            assertThat(registry.registration.metadata())
                    .containsEntry("egon.rpc.transport", "grpc")
                    .containsEntry("egon.rpc.serialization", "protobuf")
                    .containsEntry("egon.rpc.runtime-version", "test");

            lifecycle.stop();

            assertThat(registry.events)
                    .containsExactly("register", "deregister");
        }
    }

    @Test
    void disabledRegistrationMakesProvidersLocallyAvailableWithoutRegistry() {
        EgonRpcProperties properties = configuredProperties();
        properties.getProvider().setRegistrationMode(
                RpcProviderRegistrationMode.DISABLED
        );
        try (AnnotationConfigApplicationContext context = providerContext()) {
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            RpcProviderMethodRegistry methods = scan(context);
            RpcProviderLifecycle lifecycle = new RpcProviderLifecycle(
                    methods,
                    new RpcServerServiceDefinitionFactory(availability),
                    new RpcProviderServerFactory(),
                    null,
                    availability,
                    new RpcProviderServerInterceptor(),
                    properties,
                    processIdentity()
            );

            lifecycle.start();

            assertThat(methods.providers()).allSatisfy(binding ->
                    assertThat(availability.isAvailable(
                            binding.serviceIdentity()
                    )).isTrue()
            );

            lifecycle.stop();
            assertThat(methods.providers()).allSatisfy(binding ->
                    assertThat(availability.isAvailable(
                            binding.serviceIdentity()
                    )).isFalse()
            );
        }
    }

    @Test
    void requiredRegistrationFailsFastWhenRegistrySpiIsMissing() {
        try (AnnotationConfigApplicationContext context = providerContext()) {
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            RpcProviderLifecycle lifecycle = new RpcProviderLifecycle(
                    scan(context),
                    new RpcServerServiceDefinitionFactory(availability),
                    new RpcProviderServerFactory(),
                    null,
                    availability,
                    new RpcProviderServerInterceptor(),
                    configuredProperties(),
                    processIdentity()
            );

            assertThatThrownBy(lifecycle::start)
                    .isInstanceOf(EgonRpcException.class)
                    .hasMessageContaining("RpcProviderRegistry")
                    .hasMessageContaining("registration-mode=disabled");
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }

    @Test
    void failsFastForUnroutableAdvertisedHost() {
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
    void preservesNoProviderErrorSemantics() {
        RecordingRegistry registry = new RecordingRegistry();
        EgonRpcProperties properties = configuredProperties();
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProcessIdentity identity = processIdentity();
        RpcProviderLifecycle lifecycle = new RpcProviderLifecycle(
                new RpcProviderMethodRegistry(List.of()),
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                leaseManager(registry, availability, properties, identity),
                availability,
                new RpcProviderServerInterceptor(),
                properties,
                identity
        );

        assertThatThrownBy(lifecycle::start)
                .isInstanceOf(EgonRpcException.class)
                .hasMessageContaining("no RPC Provider bean");
        assertThat(registry.events).isEmpty();
    }

    @Test
    void contributorFailureStopsProviderWithoutRegisteringAnyLease() {
        RecordingRegistry registry = new RecordingRegistry();
        try (AnnotationConfigApplicationContext context = providerContext()) {
            EgonRpcProperties properties = configuredProperties();
            RpcProviderAvailabilityRegistry availability =
                    new RpcProviderAvailabilityRegistry();
            RpcProcessIdentity identity = processIdentity();
            RpcProviderMetadataMerger metadataMerger =
                    new RpcProviderMetadataMerger(List.of(service -> {
                        throw new IllegalStateException("contributor failed");
                    }));
            RpcProviderLifecycle lifecycle = new RpcProviderLifecycle(
                    scan(context),
                    new RpcServerServiceDefinitionFactory(availability),
                    new RpcProviderServerFactory(),
                    new RpcProviderLeaseManager(
                            registry,
                            availability,
                            properties,
                            identity,
                            "test",
                            metadataMerger
                    ),
                    availability,
                    new RpcProviderServerInterceptor(),
                    properties,
                    identity
            );

            assertThatThrownBy(lifecycle::start)
                    .isInstanceOf(EgonRpcException.class)
                    .hasRootCauseMessage("contributor failed");
            assertThat(registry.events).isEmpty();
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }

    private AnnotationConfigApplicationContext providerContext() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.registerBean(RpcProviderTestFixtures.EchoProvider.class);
        context.refresh();
        return context;
    }

    private RpcProviderMethodRegistry scan(
            AnnotationConfigApplicationContext context) {
        return new RpcProviderBeanScanner(
                context,
                new RpcContractValidator()
        ).scan();
    }

    private RpcProviderLifecycle lifecycle(
            AnnotationConfigApplicationContext context,
            RecordingRegistry registry,
            EgonRpcProperties properties) {
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProcessIdentity identity = processIdentity();
        return new RpcProviderLifecycle(
                scan(context),
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                leaseManager(registry, availability, properties, identity),
                availability,
                new RpcProviderServerInterceptor(),
                properties,
                identity
        );
    }

    private RpcProviderLeaseManager leaseManager(
            RpcProviderRegistry registry,
            RpcProviderAvailabilityRegistry availability,
            EgonRpcProperties properties,
            RpcProcessIdentity identity) {
        return new RpcProviderLeaseManager(
                registry,
                availability,
                properties,
                identity,
                "test",
                new RpcProviderMetadataMerger(List.of())
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
            implements RpcProviderRegistry {

        private final List<String> events = new ArrayList<>();

        private RpcProviderRegistration registration;

        @Override
        public RpcProviderLease register(
                RpcProviderRegistration registration) {
            events.add("register");
            this.registration = registration;
            Instant now = Instant.now();
            return new RpcProviderLease(
                    registration.processIdentity().instanceId(),
                    UUID.randomUUID().toString(),
                    now,
                    now.plusSeconds(registration.leaseSeconds())
            );
        }

        @Override
        public RpcLeaseOperationResult heartbeat(
                RpcProviderLeaseIdentity lease) {
            events.add("heartbeat");
            return RpcLeaseOperationResult.renewed(Instant.now());
        }

        @Override
        public RpcLeaseOperationResult deregister(
                RpcProviderLeaseIdentity lease) {
            events.add("deregister");
            return RpcLeaseOperationResult.deleted();
        }
    }
}
