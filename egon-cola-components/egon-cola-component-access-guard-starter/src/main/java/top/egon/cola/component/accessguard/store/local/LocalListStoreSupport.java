package top.egon.cola.component.accessguard.store.local;

import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class LocalListStoreSupport {

    private final Clock clock;
    private final int maxEntries;
    private final ConcurrentHashMap<EntryKey, Instant> entries = new ConcurrentHashMap<>();

    LocalListStoreSupport(Clock clock, int maxEntries) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
    }

    boolean contains(String ruleId, String dataVersion, String keyHash) {
        EntryKey key = key(ruleId, dataVersion, keyHash);
        Instant expiry = entries.get(key);
        if (expiry == null) {
            return false;
        }
        if (!expiry.isAfter(clock.instant())) {
            entries.remove(key, expiry);
            return false;
        }
        return true;
    }

    void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
        EntryKey key = key(ruleId, dataVersion, keyHash);
        entries.compute(key, (ignored, existing) -> {
            if (existing == null && entries.size() >= maxEntries) {
                throw new StoreOperationException("LIST_STORE_CAPACITY_EXCEEDED");
            }
            return expiry(ttl);
        });
    }

    void remove(String ruleId, String dataVersion, String keyHash) {
        entries.remove(key(ruleId, dataVersion, keyHash));
    }

    synchronized void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
        requireText(ruleId, "ruleId");
        requireText(dataVersion, "dataVersion");
        Set<String> hashes = Set.copyOf(keyHashes);
        hashes.forEach(LocalListStoreSupport::requireHash);
        long otherEntries = entries.keySet().stream()
                .filter(key -> !key.ruleId.equals(ruleId) || !key.dataVersion.equals(dataVersion))
                .count();
        if (otherEntries + hashes.size() > maxEntries) {
            throw new StoreOperationException("LIST_STORE_CAPACITY_EXCEEDED");
        }
        entries.keySet().removeIf(key -> key.ruleId.equals(ruleId) && key.dataVersion.equals(dataVersion));
        Instant expiry = expiry(ttl);
        hashes.forEach(hash -> entries.put(new EntryKey(ruleId, dataVersion, hash), expiry));
    }

    private EntryKey key(String ruleId, String dataVersion, String keyHash) {
        requireText(ruleId, "ruleId");
        requireText(dataVersion, "dataVersion");
        requireHash(keyHash);
        return new EntryKey(ruleId.trim(), dataVersion.trim(), keyHash);
    }

    private Instant expiry(Duration ttl) {
        Duration value = ttl == null ? Duration.ZERO : ttl;
        if (value.isNegative()) {
            throw new IllegalArgumentException("ttl must not be negative");
        }
        return value.isZero() ? Instant.MAX : clock.instant().plus(value);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireHash(String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
    }

    private record EntryKey(String ruleId, String dataVersion, String keyHash) {
    }
}
