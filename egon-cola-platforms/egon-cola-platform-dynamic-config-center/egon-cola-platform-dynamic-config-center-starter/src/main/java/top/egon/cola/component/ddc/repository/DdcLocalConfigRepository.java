package top.egon.cola.component.ddc.repository;

import org.springframework.stereotype.Repository;
import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 在线程安全的进程内结构中保存字段绑定、配置元数据和逐资源锁。
 * Stores field bindings, configuration metadata, and per-resource locks in thread-safe in-process structures.
 */
@Repository
public class DdcLocalConfigRepository {

    /** 按配置键索引的字段绑定列表。 Field-binding lists indexed by configuration key. */
    private final ConcurrentMap<String, CopyOnWriteArrayList<DdcFieldBinding>> bindings = new ConcurrentHashMap<>();

    /** 按配置键索引的本地版本。 Local versions indexed by configuration key. */
    private final ConcurrentMap<String, Long> versions = new ConcurrentHashMap<>();

    /** 按配置键索引的内容校验和。 Content checksums indexed by configuration key. */
    private final ConcurrentMap<String, String> checksums = new ConcurrentHashMap<>();

    /** 按配置资源隔离并发应用的可重入锁。 Reentrant locks serializing concurrent application per configuration resource. */
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 为配置键追加字段绑定。
     * Adds a field binding for a configuration key.
     *
     * @param key 配置键; configuration key
     * @param binding 字段绑定; field binding
     */
    public void addBinding(String key, DdcFieldBinding binding) {
        bindings.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(binding);
    }

    /**
     * 返回指定配置键的字段绑定不可变快照。
     * Returns an immutable snapshot of field bindings for a configuration key.
     *
     * @param key 配置键; configuration key
     * @return 字段绑定列表; field-binding list
     */
    public List<DdcFieldBinding> bindings(String key) {
        List<DdcFieldBinding> current = bindings.get(key);
        return current == null ? Collections.emptyList() : List.copyOf(current);
    }

    /**
     * 返回全部字段绑定的不可变快照。
     * Returns an immutable snapshot of all field bindings.
     *
     * @return 全部字段绑定; all field bindings
     */
    public List<DdcFieldBinding> bindings() {
        return bindings.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 读取配置键的本地版本。
     * Reads the local version for a configuration key.
     *
     * @param key 配置键; configuration key
     * @return 本地版本，未记录时为 {@code null}; local version, or {@code null} when absent
     */
    public Long version(String key) {
        return versions.get(key);
    }

    /**
     * 更新配置键的本地版本。
     * Updates the local version for a configuration key.
     *
     * @param key 配置键; configuration key
     * @param version 本地版本; local version
     */
    public void updateVersion(String key, long version) {
        versions.put(key, version);
    }

    /**
     * 读取配置键的内容校验和。
     * Reads the content checksum for a configuration key.
     *
     * @param key 配置键; configuration key
     * @return 内容校验和，未记录时为 {@code null}; content checksum, or {@code null} when absent
     */
    public String checksum(String key) {
        return checksums.get(key);
    }

    /**
     * 更新或删除配置键的内容校验和。
     * Updates or removes the content checksum for a configuration key.
     *
     * @param key 配置键; configuration key
     * @param checksum 内容校验和，为 {@code null} 时删除; content checksum, removed when {@code null}
     */
    public void updateChecksum(String key, String checksum) {
        if (checksum == null) {
            checksums.remove(key);
        } else {
            checksums.put(key, checksum);
        }
    }

    /**
     * 将配置键的版本和校验和恢复到指定值。
     * Restores a configuration key's version and checksum to the specified values.
     *
     * @param key 配置键; configuration key
     * @param version 待恢复版本，为 {@code null} 时删除; version to restore, removed when {@code null}
     * @param checksum 待恢复校验和，为 {@code null} 时删除; checksum to restore, removed when {@code null}
     */
    public void restoreMetadata(String key, Long version, String checksum) {
        if (version == null) {
            versions.remove(key);
        } else {
            versions.put(key, version);
        }
        updateChecksum(key, checksum);
    }

    /**
     * 在指定配置资源的独占进程内锁中执行操作。
     * Executes an action under the exclusive in-process lock for a configuration resource.
     *
     * @param key 配置资源键; configuration resource key
     * @param action 受锁保护的操作; action protected by the lock
     * @param <T> 操作返回类型; action result type
     * @return 操作结果; action result
     */
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
