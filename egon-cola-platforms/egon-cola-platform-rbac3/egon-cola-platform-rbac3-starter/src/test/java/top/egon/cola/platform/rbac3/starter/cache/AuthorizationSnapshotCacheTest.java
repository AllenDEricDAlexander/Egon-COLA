package top.egon.cola.platform.rbac3.starter.cache;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationSnapshotCacheTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @Test
    void nearCacheNeverExtendsTheAuthoritativeStoreTtl() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryStore store = new InMemoryStore(clock);
        AuthorizationSnapshotCache cache = new AuthorizationSnapshotCache(
                store, clock, Duration.ofSeconds(5));
        var key = new AuthorizationSnapshotCache.Key("finance", "tenant-a", "alice-sub");

        cache.put(key, snapshot("alice-sub"), Duration.ofSeconds(2));
        assertThat(cache.get(key)).isPresent();

        clock.advance(Duration.ofSeconds(3));

        assertThat(cache.get(key)).isEmpty();
        assertThat(store.get(key)).isEmpty();
    }

    public static SystemAuthorizationSnapshot snapshot(String identitySub) {
        return new SystemAuthorizationSnapshot(
                "tenant-a", identitySub, "101", "finance",
                7, 11, java.util.List.of("role-1"),
                java.util.Set.of("payment:read"), Map.of(), Map.of(),
                "sha256:" + identitySub, NOW, NOW.plusSeconds(3600));
    }

    public static final class InMemoryStore
            implements AuthorizationSnapshotCache.SnapshotStore {

        private final MutableClock clock;
        private final Map<AuthorizationSnapshotCache.Key, Entry> values = new HashMap<>();

        public InMemoryStore(MutableClock clock) {
            this.clock = clock;
        }

        @Override
        public synchronized Optional<SystemAuthorizationSnapshot> get(
                AuthorizationSnapshotCache.Key key) {
            Entry entry = values.get(key);
            if (entry == null || !entry.expiresAt().isAfter(clock.instant())) {
                values.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry.snapshot());
        }

        @Override
        public synchronized void put(
                AuthorizationSnapshotCache.Key key,
                SystemAuthorizationSnapshot snapshot,
                Duration ttl) {
            values.put(key, new Entry(snapshot, clock.instant().plus(ttl)));
        }

        @Override
        public synchronized void invalidate(AuthorizationSnapshotCache.Key key) {
            values.remove(key);
        }

        @Override
        public synchronized void invalidateUser(
                String systemCode, String tenantId, String identitySub) {
            values.entrySet().removeIf(entry ->
                    entry.getKey().systemCode().equals(systemCode)
                            && entry.getKey().tenantId().equals(tenantId)
                            && entry.getValue().snapshot().identitySub().equals(identitySub));
        }

        @Override
        public synchronized void invalidateTenant(String systemCode, String tenantId) {
            values.keySet().removeIf(key -> key.systemCode().equals(systemCode)
                    && key.tenantId().equals(tenantId));
        }

        private record Entry(SystemAuthorizationSnapshot snapshot, Instant expiresAt) {
        }
    }

    public static final class MutableClock extends Clock {

        private Instant current;

        public MutableClock(Instant current) {
            this.current = current;
        }

        public void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
