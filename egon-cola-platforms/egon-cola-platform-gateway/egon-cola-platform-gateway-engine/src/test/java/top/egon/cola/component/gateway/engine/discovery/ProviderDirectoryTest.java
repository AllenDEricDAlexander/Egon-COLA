package top.egon.cola.component.gateway.engine.discovery;

import org.junit.jupiter.api.Test;
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
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderDirectoryTest {

    @Test
    void sharesSubscriptionsAndDropsExpiredLeases() {
        ProviderServiceKey key = key();
        FakeRegistry registry = new FakeRegistry(key);
        ProviderDirectory directory = new ProviderDirectory(
                registry,
                Clock.fixed(
                        Instant.parse("2026-07-25T00:00:10Z"),
                        ZoneOffset.UTC
                )
        );

        directory.activate(Set.of(key));
        directory.activate(Set.of(key));

        assertEquals(2, directory.referenceCount(key));
        assertEquals(1, directory.available(key).size());
        assertTrue(directory.allAvailable(Set.of(key)));

        directory.release(Set.of(key));
        assertEquals(1, directory.referenceCount(key));
        directory.release(Set.of(key));
        assertEquals(0, directory.referenceCount(key));
        assertFalse(directory.allAvailable(Set.of(key)));
        assertTrue(registry.closed.get());
    }

    @Test
    void exposesOnlyOnlineDdcInstancesWithCurrentLeasesAsRegistered() {
        ProviderServiceKey providerKey = key();
        DdcServiceKey ddcKey = new DdcServiceKey(
                "test-biz", "local", "test-app",
                DdcServiceKind.HTTP_PROVIDER,
                "orders", "default", "v1", "http"
        );
        Instant now = Instant.now();
        DdcSnapshotRegistry registry = new DdcSnapshotRegistry(
                new DdcServiceSnapshot(
                        ddcKey,
                        1,
                        List.of(
                                ddcInstance(ddcKey, "online", "ONLINE",
                                        now.plusSeconds(60)),
                                ddcInstance(ddcKey, "legacy", "REGISTERED",
                                        now.plusSeconds(60)),
                                ddcInstance(ddcKey, "expired", "ONLINE",
                                        now.minusSeconds(1)),
                                ddcInstance(ddcKey, "unknown", null,
                                        now.plusSeconds(60))
                        ),
                        now
                )
        );

        ProviderServiceSnapshot snapshot = new DdcProviderServiceRegistryAdapter(registry)
                .getInstances(providerKey);

        assertEquals(providerKey, snapshot.serviceKey());
        assertEquals(
                ProviderRegistryState.REGISTERED,
                state(snapshot, "online")
        );
        assertEquals(
                ProviderRegistryState.REGISTERED,
                state(snapshot, "legacy")
        );
        assertEquals(
                ProviderRegistryState.EXPIRED,
                state(snapshot, "expired")
        );
        assertEquals(
                ProviderRegistryState.EXPIRED,
                state(snapshot, "unknown")
        );
    }

    private ProviderServiceKey key() {
        return new ProviderServiceKey(
                "test-biz",
                "test-app",
                "local",
                "default",
                ProviderProtocolType.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
    }

    private DdcServiceInstance ddcInstance(
            DdcServiceKey key,
            String instanceId,
            String status,
            Instant leaseExpireAt) {
        return new DdcServiceInstance(
                instanceId,
                "lease-" + instanceId,
                key,
                "127.0.0.1",
                8080,
                false,
                java.util.Map.of(),
                30,
                10,
                leaseExpireAt.minusSeconds(30),
                leaseExpireAt.minusSeconds(10),
                leaseExpireAt,
                status,
                1
        );
    }

    private ProviderRegistryState state(
            ProviderServiceSnapshot snapshot,
            String instanceId) {
        return snapshot.instances().stream()
                .filter(instance -> instance.instanceId().equals(instanceId))
                .findFirst()
                .orElseThrow()
                .registryState();
    }

    private static final class FakeRegistry
            implements ProviderServiceRegistry {

        private final ProviderServiceKey key;

        private final AtomicBoolean closed = new AtomicBoolean();

        private FakeRegistry(ProviderServiceKey key) {
            this.key = key;
        }

        @Override
        public ProviderCatalogSnapshot getServiceKeys(ProviderQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProviderServiceSnapshot getInstances(ProviderServiceKey key) {
            return new ProviderServiceSnapshot(
                    key,
                    1,
                    Instant.parse("2026-07-25T00:00:00Z"),
                    List.of(new ProviderInstance(
                            key,
                            "a",
                            "lease",
                            "127.0.0.1",
                            8080,
                            false,
                            java.util.Map.of(),
                            Instant.parse("2026-07-25T00:00:30Z"),
                            ProviderRegistryState.REGISTERED,
                            ProviderHealthState.HEALTHY,
                            ProviderHealthState.HEALTHY
                    ))
            );
        }

        @Override
        public ProviderSubscription subscribeServices(
                ProviderQuery query,
                ProviderCatalogListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProviderSubscription subscribe(
                ProviderServiceKey key,
                ProviderSnapshotListener listener) {
            return new ProviderSubscription() {
                @Override
                public boolean active() {
                    return !closed.get();
                }

                @Override
                public void close() {
                    closed.set(true);
                }
            };
        }
    }

    private static final class DdcSnapshotRegistry
            implements DdcServiceRegistryClient {

        private final DdcServiceSnapshot snapshot;

        private DdcSnapshotRegistry(DdcServiceSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            return snapshot;
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                java.util.function.Consumer<DdcServiceSnapshot> listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                java.util.function.Consumer<DdcServiceCatalogSnapshot> listener) {
            throw new UnsupportedOperationException();
        }
    }
}
