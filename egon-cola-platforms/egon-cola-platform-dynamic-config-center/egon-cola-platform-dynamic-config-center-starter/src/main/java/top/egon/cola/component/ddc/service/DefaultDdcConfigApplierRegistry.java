package top.egon.cola.component.ddc.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultDdcConfigApplierRegistry implements DdcConfigApplierRegistry {

    private final DdcConfigApplier fallback;

    private final Map<String, DdcConfigApplier> exactAppliers = new LinkedHashMap<>();

    private final Map<String, DdcConfigApplier> prefixAppliers = new LinkedHashMap<>();

    private volatile Snapshot snapshot;

    public DefaultDdcConfigApplierRegistry(DdcConfigApplier fallback) {
        this.fallback = requireApplier(fallback);
    }

    @Override
    public synchronized void registerExact(String configKey, DdcConfigApplier applier) {
        requireMutable();
        String key = requireKey(configKey, "configKey");
        requireApplier(applier);
        if (exactAppliers.putIfAbsent(key, applier) != null) {
            throw new IllegalArgumentException("DDC exact config applier already registered: " + key);
        }
    }

    @Override
    public synchronized void registerPrefix(String configKeyPrefix, DdcConfigApplier applier) {
        requireMutable();
        String prefix = requireKey(configKeyPrefix, "configKeyPrefix");
        requireApplier(applier);
        if (!prefix.endsWith(".")) {
            throw new IllegalArgumentException("DDC config key prefix must end with '.': " + prefix);
        }
        if (prefixAppliers.putIfAbsent(prefix, applier) != null) {
            throw new IllegalArgumentException("DDC prefix config applier already registered: " + prefix);
        }
    }

    @Override
    public DdcConfigApplier resolve(String configKey) {
        String key = requireKey(configKey, "configKey");
        Snapshot current = snapshot;
        if (current != null) {
            return resolve(current, key);
        }
        synchronized (this) {
            current = snapshot;
            return resolve(current == null ? mutableSnapshot() : current, key);
        }
    }

    @Override
    public boolean hasExplicitRegistration(String configKey) {
        String key = requireKey(configKey, "configKey");
        Snapshot current = snapshot;
        if (current != null) {
            return current.resolve(key) != null;
        }
        synchronized (this) {
            current = snapshot;
            return (current == null ? mutableSnapshot() : current)
                    .resolve(key) != null;
        }
    }

    public synchronized void freeze() {
        if (snapshot == null) {
            snapshot = immutableSnapshot();
        }
    }

    public boolean frozen() {
        return snapshot != null;
    }

    private Snapshot mutableSnapshot() {
        return new Snapshot(exactAppliers, sortedPrefixes());
    }

    private Snapshot immutableSnapshot() {
        return new Snapshot(Map.copyOf(exactAppliers), List.copyOf(sortedPrefixes()));
    }

    private DdcConfigApplier resolve(Snapshot current, String configKey) {
        DdcConfigApplier resolved = current.resolve(configKey);
        return resolved == null ? fallback : resolved;
    }

    private List<PrefixApplier> sortedPrefixes() {
        List<PrefixApplier> prefixes = new ArrayList<>();
        prefixAppliers.forEach((prefix, applier) -> prefixes.add(new PrefixApplier(prefix, applier)));
        prefixes.sort(Comparator.comparingInt((PrefixApplier item) -> item.prefix().length()).reversed());
        return prefixes;
    }

    private void requireMutable() {
        if (snapshot != null) {
            throw new IllegalStateException("DDC config applier registry is frozen");
        }
    }

    private String requireKey(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private DdcConfigApplier requireApplier(DdcConfigApplier applier) {
        if (applier == null) {
            throw new IllegalArgumentException("applier must not be null");
        }
        return applier;
    }

    private record Snapshot(
            Map<String, DdcConfigApplier> exactAppliers,
            List<PrefixApplier> prefixAppliers
    ) {

        private DdcConfigApplier resolve(String configKey) {
            DdcConfigApplier exact = exactAppliers.get(configKey);
            if (exact != null) {
                return exact;
            }
            for (PrefixApplier candidate : prefixAppliers) {
                if (configKey.startsWith(candidate.prefix())) {
                    return candidate.applier();
                }
            }
            return null;
        }
    }

    private record PrefixApplier(String prefix, DdcConfigApplier applier) {
    }
}
