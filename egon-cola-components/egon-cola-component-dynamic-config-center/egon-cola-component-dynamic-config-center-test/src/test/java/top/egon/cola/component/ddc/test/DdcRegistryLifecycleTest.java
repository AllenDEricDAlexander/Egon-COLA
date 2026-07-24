package top.egon.cola.component.ddc.test;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRegistryLifecycleTest {

    @Test
    void providerAndGatewayFollowRegisterSubscribeHeartbeatDeregisterLifecycle() {
        InMemoryRegistry registry = new InMemoryRegistry();

        verifyLifecycle(
                registry,
                registration("provider-1", DdcServiceKind.RPC_PROVIDER, 19091)
        );
        verifyLifecycle(
                registry,
                registration("gateway-1", DdcServiceKind.INTERNAL_GATEWAY, 19090)
        );
    }

    @Test
    void serviceRegistryIsRedisOnlyAndDoesNotIntroduceJpaTables() throws IOException {
        assertThat(DdcServiceInstance.class.isAnnotationPresent(Entity.class)).isFalse();
        assertThat(DdcServiceRegistryRedisRepository.class.isAnnotationPresent(Entity.class))
                .isFalse();

        for (String dialect : List.of("postgresql", "sqlite")) {
            String v1 = resource("db/" + dialect + "/V1__create_ddc_schema.sql");
            String v2 = resource("db/" + dialect + "/V2__add_lease_and_sync_publish.sql");
            assertThat(v1 + v2)
                    .doesNotContain("ddc_service_instance")
                    .doesNotContain("ddc_service_registry");
        }
    }

    private void verifyLifecycle(InMemoryRegistry registry,
                                 DdcServiceRegistration registration) {
        DdcLeaseSession session = registry.register(registration);
        List<DdcServiceSnapshot> snapshots = new ArrayList<>();
        DdcRegistrySubscription subscription =
                registry.subscribe(registration.serviceKey(), snapshots::add);

        assertThat(snapshots.getLast().instances())
                .extracting(DdcServiceInstance::instanceId)
                .containsExactly(registration.instanceId());
        assertThat(registry.heartbeat(
                session.instanceId(),
                session.leaseId()
        ).status()).isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(registry.deregister(
                session.instanceId(),
                session.leaseId()
        ).status()).isEqualTo(DdcLeaseOperationStatus.DELETED);
        assertThat(snapshots.getLast().instances()).isEmpty();

        subscription.close();
    }

    private DdcServiceRegistration registration(String instanceId,
                                                DdcServiceKind kind,
                                                int port) {
        return new DdcServiceRegistration(
                instanceId,
                new DdcServiceKey(
                        "dev",
                        "default",
                        kind,
                        kind == DdcServiceKind.RPC_PROVIDER
                                ? "demo.GreetingService"
                                : "internal-gateway",
                        "default",
                        "1.0.0",
                        "grpc"
                ),
                "127.0.0.1",
                port,
                false,
                Map.of("zone", "local"),
                kind == DdcServiceKind.RPC_PROVIDER ? 30 : 15,
                kind == DdcServiceKind.RPC_PROVIDER ? 10 : 5
        );
    }

    private String resource(String path) throws IOException {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static final class InMemoryRegistry implements DdcServiceRegistryClient {

        private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

        private final AtomicInteger leaseSequence = new AtomicInteger();

        private final Map<String, DdcServiceInstance> instances = new LinkedHashMap<>();

        private final Map<DdcServiceKey, List<Consumer<DdcServiceSnapshot>>> listeners =
                new LinkedHashMap<>();

        private long revision;

        @Override
        public synchronized DdcLeaseSession register(
                DdcServiceRegistration registration) {
            String leaseId = "lease-" + leaseSequence.incrementAndGet();
            Instant expiresAt = NOW.plusSeconds(registration.leaseSeconds());
            DdcServiceInstance instance = new DdcServiceInstance(
                    registration.instanceId(),
                    leaseId,
                    registration.serviceKey(),
                    registration.host(),
                    registration.port(),
                    registration.secure(),
                    registration.metadata(),
                    registration.leaseSeconds(),
                    registration.heartbeatIntervalSeconds(),
                    NOW,
                    NOW,
                    expiresAt,
                    "UP",
                    ++revision
            );
            instances.put(registration.instanceId(), instance);
            notifyListeners(registration.serviceKey());
            return new DdcLeaseSession(
                    registration.instanceId(),
                    leaseId,
                    registration.serviceKey().serviceKind().leaseRole(),
                    registration.leaseSeconds(),
                    registration.heartbeatIntervalSeconds(),
                    NOW,
                    expiresAt
            );
        }

        @Override
        public synchronized DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            DdcServiceInstance current = instances.get(instanceId);
            if (current == null) {
                return new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.NOT_FOUND,
                        null
                );
            }
            if (!current.leaseId().equals(leaseId)) {
                return new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.LEASE_MISMATCH,
                        null
                );
            }
            Instant expiresAt = NOW.plusSeconds(current.leaseSeconds());
            instances.put(instanceId, new DdcServiceInstance(
                    current.instanceId(),
                    current.leaseId(),
                    current.serviceKey(),
                    current.host(),
                    current.port(),
                    current.secure(),
                    current.metadata(),
                    current.leaseSeconds(),
                    current.heartbeatIntervalSeconds(),
                    current.registeredAt(),
                    NOW,
                    expiresAt,
                    current.status(),
                    ++revision
            ));
            notifyListeners(current.serviceKey());
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    expiresAt
            );
        }

        @Override
        public synchronized DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            DdcServiceInstance current = instances.get(instanceId);
            if (current == null) {
                return new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.NOT_FOUND,
                        null
                );
            }
            if (!current.leaseId().equals(leaseId)) {
                return new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.NOT_DELETED,
                        null
                );
            }
            instances.remove(instanceId);
            revision++;
            notifyListeners(current.serviceKey());
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.DELETED,
                    null
            );
        }

        @Override
        public synchronized DdcServiceSnapshot getInstances(
                DdcServiceKey serviceKey) {
            return new DdcServiceSnapshot(
                    serviceKey,
                    revision,
                    instances.values().stream()
                            .filter(instance -> instance.serviceKey().equals(serviceKey))
                            .toList(),
                    NOW
            );
        }

        @Override
        public synchronized DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            listeners.computeIfAbsent(serviceKey, ignored -> new ArrayList<>())
                    .add(listener);
            listener.accept(getInstances(serviceKey));
            return () -> {
                synchronized (InMemoryRegistry.this) {
                    listeners.getOrDefault(serviceKey, List.of()).remove(listener);
                }
            };
        }

        @Override
        public synchronized DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            return new DdcServiceCatalogSnapshot(
                    query,
                    revision,
                    instances.values().stream()
                            .map(DdcServiceInstance::serviceKey)
                            .filter(query::matches)
                            .distinct()
                            .toList(),
                    NOW
            );
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            listener.accept(getServiceKeys(query));
            return () -> {
            };
        }

        private void notifyListeners(DdcServiceKey serviceKey) {
            DdcServiceSnapshot snapshot = getInstances(serviceKey);
            List.copyOf(listeners.getOrDefault(serviceKey, List.of()))
                    .forEach(listener -> listener.accept(snapshot));
        }
    }
}
