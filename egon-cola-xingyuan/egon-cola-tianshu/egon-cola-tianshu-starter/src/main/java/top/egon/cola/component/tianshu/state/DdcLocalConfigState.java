package top.egon.cola.component.tianshu.state;

import org.springframework.lang.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在线程安全的进程内结构中保存配置元数据和逐资源锁。
 * Stores configuration metadata and per-resource locks in thread-safe in-process structures.
 */
public class DdcLocalConfigState {

    private final Long tenantId;

    /**
     * 按配置资源名索引的本地版本。 Local versions indexed by configuration resource name.
     */
    private final ConcurrentMap<ResourceKey, Long> versions = new ConcurrentHashMap<>();

    /**
     * 按配置资源名索引的资源校验和。 Resource checksums indexed by configuration resource name.
     */
    private final ConcurrentMap<ResourceKey, String> checksums = new ConcurrentHashMap<>();

    /**
     * 按配置资源隔离并发应用的可重入锁。 Reentrant locks serializing concurrent application per configuration resource.
     */
    private final ConcurrentMap<ResourceKey, ReentrantLock> locks = new ConcurrentHashMap<>();

    public DdcLocalConfigState(Long tenantId) {
        if (tenantId != null && tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        this.tenantId = tenantId;
    }

    /**
     * 读取配置资源的本地版本。
     * Reads the local version for a configuration resource.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @return 本地版本，未记录时为 {@code null}; local version, or {@code null} when absent
     */
    @Nullable
    public Long version(String resourceName) {
        return versions.get(resourceKey(resourceName));
    }

    /**
     * 更新配置资源的本地版本。
     * Updates the local version for a configuration resource.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @param version      本地版本; local version
     */
    public void updateVersion(String resourceName, long version) {
        versions.put(resourceKey(resourceName), version);
    }

    /**
     * 读取配置资源的资源校验和。
     * Reads the resource checksum for a configuration resource.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @return 资源校验和，未记录时为 {@code null}; resource checksum, or {@code null} when absent
     */
    @Nullable
    public String checksum(String resourceName) {
        return checksums.get(resourceKey(resourceName));
    }

    /**
     * 更新或删除配置资源的资源校验和。
     * Updates or removes the resource checksum for a configuration resource.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @param checksum     资源校验和，为 {@code null} 时删除; resource checksum, removed when {@code null}
     */
    public void updateChecksum(String resourceName,
                               @Nullable String checksum) {
        if (checksum == null) {
            checksums.remove(resourceKey(resourceName));
        } else {
            checksums.put(resourceKey(resourceName), checksum);
        }
    }

    /**
     * 将配置资源的版本和校验和恢复到指定值。
     * Restores a configuration resource's version and checksum to the specified values.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @param version      待恢复版本，为 {@code null} 时删除; version to restore, removed when {@code null}
     * @param checksum     待恢复校验和，为 {@code null} 时删除; checksum to restore, removed when {@code null}
     */
    public void restoreMetadata(String resourceName,
                                @Nullable Long version,
                                @Nullable String checksum) {
        if (version == null) {
            versions.remove(resourceKey(resourceName));
        } else {
            versions.put(resourceKey(resourceName), version);
        }
        updateChecksum(resourceName, checksum);
    }

    /**
     * 在指定配置资源的独占进程内锁中执行操作。
     * Executes an action under the exclusive in-process lock for a configuration resource.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @param action       受锁保护的操作; action protected by the lock
     * @param <T>          操作返回类型; action result type
     * @return 操作结果; action result
     */
    public <T> T withConfigLock(String resourceName, Supplier<T> action) {
        ResourceKey key = resourceKey(resourceName);
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private ResourceKey resourceKey(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("A positive configured tenant is required for local Tianshu state");
        }
        return new ResourceKey(tenantId, resourceName);
    }

    private record ResourceKey(long tenantId, String resourceName) {
    }
}
