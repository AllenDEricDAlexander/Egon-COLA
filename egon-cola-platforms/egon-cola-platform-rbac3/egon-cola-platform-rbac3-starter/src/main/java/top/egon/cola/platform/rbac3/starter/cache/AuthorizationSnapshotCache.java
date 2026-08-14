package top.egon.cola.platform.rbac3.starter.cache;

import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-level cache keyed by system, tenant and USER subject.
 */
public final class AuthorizationSnapshotCache {
    private final SnapshotStore store;
    private final Clock clock;
    private final Duration nearTtl;
    private final ConcurrentHashMap<Key, NearEntry> near = new ConcurrentHashMap<>();

    public AuthorizationSnapshotCache(SnapshotStore store, Clock clock, Duration nearTtl) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.nearTtl = bounded(nearTtl, Duration.ZERO, Duration.ofSeconds(5), "nearTtl");
    }

    public Optional<SystemAuthorizationSnapshot> get(Key key) {
        Objects.requireNonNull(key, "key");
        Instant now = clock.instant();
        NearEntry local = near.get(key);
        if (local != null) {
            if (local.validAt(now)) return Optional.of(local.snapshot());
            near.remove(key, local);
        }
        Optional<SystemAuthorizationSnapshot> stored = store.get(key)
                .filter(snapshot -> validBinding(key, snapshot))
                .filter(snapshot -> snapshot.expiresAt().isAfter(now));
        stored.ifPresent(snapshot -> near.put(key, new NearEntry(snapshot,
                minimum(now.plus(nearTtl), snapshot.expiresAt()))));
        return stored;
    }

    public void put(Key key, SystemAuthorizationSnapshot snapshot, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!validBinding(key, snapshot)) throw new IllegalArgumentException("snapshot does not match cache key");
        Instant now = clock.instant();
        if (!snapshot.expiresAt().isAfter(now)) throw new IllegalArgumentException("snapshot has expired");
        Duration effectiveTtl = minimum(bounded(ttl, Duration.ofMillis(1), Duration.ofMinutes(10), "ttl"),
                Duration.between(now, snapshot.expiresAt()));
        store.put(key, snapshot, effectiveTtl);
        near.put(key, new NearEntry(snapshot, minimum(now.plus(nearTtl), now.plus(effectiveTtl), snapshot.expiresAt())));
    }

    public void invalidate(Key key) {
        near.remove(Objects.requireNonNull(key, "key"));
        store.invalidate(key);
    }

    public void invalidateUser(String systemCode, String tenantId, String identitySub) {
        String system = required(systemCode, "systemCode");
        String tenant = required(tenantId, "tenantId");
        String subject = required(identitySub, "identitySub");
        near.entrySet().removeIf(entry -> entry.getKey().systemCode().equals(system)
                && entry.getKey().tenantId().equals(tenant)
                && entry.getKey().identitySub().equals(subject));
        store.invalidateUser(system, tenant, subject);
    }

    public void invalidateTenant(String systemCode, String tenantId) {
        String system = required(systemCode, "systemCode");
        String tenant = required(tenantId, "tenantId");
        near.keySet().removeIf(key -> key.systemCode().equals(system) && key.tenantId().equals(tenant));
        store.invalidateTenant(system, tenant);
    }

    private boolean validBinding(Key key, SystemAuthorizationSnapshot snapshot) {
        return key.systemCode().equals(snapshot.systemCode())
                && key.tenantId().equals(snapshot.tenantId())
                && key.identitySub().equals(snapshot.identitySub());
    }

    private static Duration bounded(Duration value, Duration minimum, Duration maximum, String name) {
        Objects.requireNonNull(value, name);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " is outside the safe range");
        }
        return value;
    }

    private static Duration minimum(Duration... values) {
        Duration result = values[0];
        for (Duration value : values) if (value.compareTo(result) < 0) result = value;
        return result;
    }

    private static Instant minimum(Instant... values) {
        Instant result = values[0];
        for (Instant value : values) if (value.isBefore(result)) result = value;
        return result;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    public record Key(String systemCode, String tenantId, String identitySub) {
        public Key {
            systemCode = required(systemCode, "systemCode");
            tenantId = required(tenantId, "tenantId");
            identitySub = required(identitySub, "identitySub");
        }

        public String redisKey() {
            return "rbac3:authorization:" + systemCode + ':' + tenantId + ':' + identitySub;
        }

        public String userIndex(String subject) {
            return "rbac3:authorization:" + systemCode + ':' + tenantId + ":user:" + required(subject, "identitySub");
        }

        public String tenantIndex() {
            return "rbac3:authorization:" + systemCode + ':' + tenantId + ":tenant";
        }
    }

    private record NearEntry(SystemAuthorizationSnapshot snapshot, Instant expiresAt) {
        boolean validAt(Instant now) {
            return expiresAt.isAfter(now) && snapshot.expiresAt().isAfter(now);
        }
    }

    public interface SnapshotStore {
        Optional<SystemAuthorizationSnapshot> get(Key key);
        void put(Key key, SystemAuthorizationSnapshot snapshot, Duration ttl);
        void invalidate(Key key);
        void invalidateUser(String systemCode, String tenantId, String identitySub);
        void invalidateTenant(String systemCode, String tenantId);
    }
}
