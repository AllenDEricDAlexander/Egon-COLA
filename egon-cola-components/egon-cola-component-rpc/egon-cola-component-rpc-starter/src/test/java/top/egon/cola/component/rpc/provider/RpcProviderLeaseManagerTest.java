package top.egon.cola.component.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProviderLeaseManagerTest {

    @Test
    void replacesLostLeaseBeforeRestoringAvailability() {
        RecordingRegistry registry = new RecordingRegistry();
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProviderLeaseManager leases = manager(registry, availability);
        List<RpcProviderBinding> providers = providers();
        leases.prepare(providers, "127.0.0.1", 19090);
        leases.enableRecovery();
        leases.registerAll();
        String firstLease = leases.currentLeases().values()
                .iterator()
                .next()
                .leaseId();
        registry.nextHeartbeat =
                RpcLeaseOperationResult.leaseMismatch();

        leases.heartbeatAndRecover();

        String secondLease = leases.currentLeases().values()
                .iterator()
                .next()
                .leaseId();
        assertThat(secondLease).isNotEqualTo(firstLease);
        assertThat(registry.events)
                .containsExactly("register", "heartbeat", "register");
        assertThat(providers).allSatisfy(provider ->
                assertThat(availability.isAvailable(
                        provider.serviceIdentity()
                )).isTrue()
        );
    }

    @Test
    void disablesRecoveryBeforeDeregisteringDuringHeartbeat() {
        RecordingRegistry registry = new RecordingRegistry();
        registry.blockHeartbeat = true;
        RpcProviderLeaseManager leases = manager(
                registry,
                new RpcProviderAvailabilityRegistry()
        );
        leases.prepare(providers(), "127.0.0.1", 19090);
        leases.enableRecovery();
        leases.registerAll();
        registry.nextHeartbeat =
                RpcLeaseOperationResult.leaseMismatch();

        CompletableFuture<Void> heartbeat = CompletableFuture.runAsync(
                leases::heartbeatAndRecover
        );
        registry.awaitHeartbeat();
        CompletableFuture<Void> disable = CompletableFuture.runAsync(
                leases::disableRecovery
        );

        assertThat(disable).isNotDone();
        registry.releaseHeartbeat();
        heartbeat.join();
        disable.join();
        leases.deregisterAll();
        leases.heartbeatAndRecover();

        assertThat(registry.events)
                .containsExactly(
                        "register",
                        "heartbeat",
                        "register",
                        "deregister"
                );
    }

    @Test
    void sendsOnlyRpcSemanticRegistrationData() {
        RecordingRegistry registry = new RecordingRegistry();
        RpcProviderLeaseManager leases = manager(
                registry,
                new RpcProviderAvailabilityRegistry()
        );
        leases.prepare(providers(), "127.0.0.1", 19090);
        leases.enableRecovery();

        leases.registerAll();

        assertThat(registry.registration.host()).isEqualTo("127.0.0.1");
        assertThat(registry.registration.port()).isEqualTo(19090);
        assertThat(registry.registration.secure()).isFalse();
        assertThat(registry.registration.serviceIdentity())
                .isEqualTo(new RpcServiceIdentity(
                        "egon.rpc.fixture.v1.UnaryFixtureService",
                        "test",
                        "1.0.0"
                ));
        assertThat(registry.registration.metadata())
                .containsEntry("egon.rpc.runtime-version", "test");
    }

    private RpcProviderLeaseManager manager(
            RpcProviderRegistry registry,
            RpcProviderAvailabilityRegistry availability) {
        return new RpcProviderLeaseManager(
                registry,
                availability,
                new EgonRpcProperties(),
                new RpcProcessIdentity(
                        "provider-test",
                        "test",
                        "default",
                        "127.0.0.1",
                        1,
                        "provider-process"
                ),
                "test",
                new RpcProviderMetadataMerger(List.of())
        );
    }

    private List<RpcProviderBinding> providers() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(RpcProviderTestFixtures.EchoProvider.class);
            context.refresh();
            return new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            ).scan().providers();
        }
    }

    private static final class RecordingRegistry
            implements RpcProviderRegistry {

        private final List<String> events = new ArrayList<>();

        private RpcProviderRegistration registration;

        private RpcLeaseOperationResult nextHeartbeat =
                RpcLeaseOperationResult.renewed(Instant.now());

        private boolean blockHeartbeat;

        private final CountDownLatch heartbeatStarted =
                new CountDownLatch(1);

        private final CountDownLatch heartbeatReleased =
                new CountDownLatch(1);

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
            RpcLeaseOperationResult result = nextHeartbeat;
            nextHeartbeat = RpcLeaseOperationResult.renewed(Instant.now());
            return result;
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
        public RpcLeaseOperationResult deregister(
                RpcProviderLeaseIdentity lease) {
            events.add("deregister");
            return RpcLeaseOperationResult.deleted();
        }
    }
}
