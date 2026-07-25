package top.egon.cola.component.ddc.repository;

import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class DdcLocalConfigRepository {

    private final ConcurrentMap<String, CopyOnWriteArrayList<DdcFieldBinding>> bindings = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Long> versions = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> checksums = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void addBinding(String key, DdcFieldBinding binding) {
        bindings.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(binding);
    }

    public List<DdcFieldBinding> bindings(String key) {
        List<DdcFieldBinding> current = bindings.get(key);
        return current == null ? Collections.emptyList() : List.copyOf(current);
    }

    public List<DdcFieldBinding> bindings() {
        return bindings.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public Long version(String key) {
        return versions.get(key);
    }

    public void updateVersion(String key, long version) {
        versions.put(key, version);
    }

    public String checksum(String key) {
        return checksums.get(key);
    }

    public void updateChecksum(String key, String checksum) {
        if (checksum == null) {
            checksums.remove(key);
        } else {
            checksums.put(key, checksum);
        }
    }

    public void restoreMetadata(String key, Long version, String checksum) {
        if (version == null) {
            versions.remove(key);
        } else {
            versions.put(key, version);
        }
        updateChecksum(key, checksum);
    }

    public <T> T withConfigLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
