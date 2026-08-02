package top.egon.cola.platform.rbac3.starter.cache;

import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downstream-owned two-level authorization cache whose near entries never
 * outlive either the Redis TTL or the snapshot expiry.
 */
public final class AuthorizationSnapshotCache {

    private final SnapshotStore store;
    private final Clock clock;
    private final Duration nearTtl;
    private final ConcurrentHashMap<Key, NearEntry> near = new ConcurrentHashMap<>();

    public AuthorizationSnapshotCache(
            SnapshotStore store,
            Clock clock,
            Duration nearTtl) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.nearTtl = bounded(nearTtl, Duration.ZERO, Duration.ofSeconds(5),
                "nearTtl");
    }

    public Optional<SystemAuthorizationSnapshot> get(Key key) {
        Objects.requireNonNull(key, "key");
        Instant now = clock.instant();
        NearEntry local = near.get(key);
        if (local != null) {
            if (local.validAt(now)) {
                return Optional.of(local.snapshot());
            }
            near.remove(key, local);
        }
        Optional<SystemAuthorizationSnapshot> stored = store.get(key)
                .filter(snapshot -> validBinding(key, snapshot))
                .filter(snapshot -> snapshot.expiresAt().isAfter(now));
        stored.ifPresent(snapshot -> near.put(
                key, new NearEntry(snapshot, minimum(
                        now.plus(nearTtl), snapshot.expiresAt()))));
        return stored;
    }

    public void put(
            Key key,
            SystemAuthorizationSnapshot snapshot,
            Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!validBinding(key, snapshot)) {
            throw new IllegalArgumentException("snapshot does not match cache key");
        }
        Instant now = clock.instant();
        if (!snapshot.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("snapshot has expired");
        }
        Duration boundedTtl = bounded(ttl, Duration.ofMillis(1),
                Duration.ofMinutes(10), "ttl");
        Duration effectiveTtl = minimum(
                boundedTtl, Duration.between(now, snapshot.expiresAt()));
        store.put(key, snapshot, effectiveTtl);
        near.put(key, new NearEntry(snapshot, minimum(
                now.plus(nearTtl), now.plus(effectiveTtl), snapshot.expiresAt())));
    }

    public void invalidate(Key key) {
        near.remove(Objects.requireNonNull(key, "key"));
        store.invalidate(key);
    }

    public void invalidateUser(
            String systemCode,
            String tenantId,
            String identitySub) {
        String system = required(systemCode, "systemCode");
        String tenant = required(tenantId, "tenantId");
        String subject = required(identitySub, "identitySub");
        near.entrySet().removeIf(entry -> entry.getKey().systemCode().equals(system)
                && entry.getKey().tenantId().equals(tenant)
                && entry.getValue().snapshot().identitySub().equals(subject));
        store.invalidateUser(system, tenant, subject);
    }

    public void invalidateTenant(String systemCode, String tenantId) {
        String system = required(systemCode, "systemCode");
        String tenant = required(tenantId, "tenantId");
        near.keySet().removeIf(key -> key.systemCode().equals(system)
                && key.tenantId().equals(tenant));
        store.invalidateTenant(system, tenant);
    }

    private boolean validBinding(Key key, SystemAuthorizationSnapshot snapshot) {
        return key.systemCode().equals(snapshot.systemCode())
                && key.tenantId().equals(snapshot.tenantId())
                && key.sessionId().equals(snapshot.sessionId());
    }

    private static Duration bounded(
            Duration value,
            Duration minimum,
            Duration maximum,
            String name) {
        Objects.requireNonNull(value, name);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " is outside the safe range");
        }
        return value;
    }

    private static Duration minimum(Duration left, Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static Instant minimum(Instant... values) {
        Instant result = Objects.requireNonNull(values[0], "values[0]");
        for (int index = 1; index < values.length; index++) {
            if (values[index].isBefore(result)) {
                result = values[index];
            }
        }
        return result;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    public interface SnapshotStore {

        Optional<SystemAuthorizationSnapshot> get(Key key);

        void put(Key key, SystemAuthorizationSnapshot snapshot, Duration ttl);

        void invalidate(Key key);

        void invalidateUser(String systemCode, String tenantId, String identitySub);

        void invalidateTenant(String systemCode, String tenantId);
    }

    public record Key(String systemCode, String tenantId, String sessionId) {

        public Key {
            systemCode = required(systemCode, "systemCode");
            tenantId = required(tenantId, "tenantId");
            sessionId = required(sessionId, "sessionId");
        }

        public String redisKey() {
            return "rbac3:authorization:" + systemCode + ':' + tenantId + ':' + sessionId;
        }

        public String userIndex(String identitySub) {
            return "rbac3:authorization:index:user:" + systemCode + ':'
                    + tenantId + ':' + required(identitySub, "identitySub");
        }

        public String tenantIndex() {
            return "rbac3:authorization:index:tenant:" + systemCode + ':' + tenantId;
        }
    }

    private record NearEntry(
            SystemAuthorizationSnapshot snapshot,
            Instant expiresAt) {

        private boolean validAt(Instant now) {
            return expiresAt.isAfter(now) && snapshot.expiresAt().isAfter(now);
        }
    }
}
