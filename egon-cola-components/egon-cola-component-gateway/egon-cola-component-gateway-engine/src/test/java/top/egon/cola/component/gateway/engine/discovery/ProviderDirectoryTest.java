package top.egon.cola.component.gateway.engine.discovery;

import org.junit.jupiter.api.Test;
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

    private ProviderServiceKey key() {
        return new ProviderServiceKey(
                "local",
                "default",
                ProviderProtocolType.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
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
}
