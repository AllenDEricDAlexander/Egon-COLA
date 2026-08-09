package top.egon.cola.component.ddc.service.refresh;

import top.egon.cola.component.ddc.api.refresh.DdcConfigApplier;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持精确键、最长前缀和回退应用器的默认配置应用器注册表。
 * Default configuration-applier registry supporting exact keys, longest prefixes, and a fallback applier.
 *
 * <p>冻结前允许同步注册；冻结后使用不可变快照进行无锁读取。</p>
 * <p>Registrations are synchronized before freezing; reads use an immutable lock-free snapshot afterward.</p>
 */
public class DefaultDdcConfigApplierRegistry implements DdcConfigApplierRegistry {

    /**
     * 未命中显式注册时使用的回退应用器。 Fallback applier used when no explicit registration matches.
     */
    private final DdcConfigApplier fallback;

    /**
     * 按精确配置键保存的应用器。 Appliers stored by exact configuration key.
     */
    private final Map<String, DdcConfigApplier> exactAppliers = new LinkedHashMap<>();

    /**
     * 按配置键前缀保存的应用器。 Appliers stored by configuration-key prefix.
     */
    private final Map<String, DdcConfigApplier> prefixAppliers = new LinkedHashMap<>();

    /**
     * 冻结后发布的不可变查找快照。 Immutable lookup snapshot published after freezing.
     */
    private volatile Snapshot snapshot;

    /**
     * 使用指定回退应用器创建注册表。
     * Creates the registry with the specified fallback applier.
     *
     * @param fallback 未命中显式注册时使用的应用器; applier used when no explicit registration matches
     */
    public DefaultDdcConfigApplierRegistry(DdcConfigApplier fallback) {
        this.fallback = requireApplier(fallback);
    }

    /**
     * {@inheritDoc} 中文：注册精确键应用器并拒绝重复键。 English: Registers an exact-key applier and rejects duplicates.
     */
    @Override
    public synchronized void registerExact(String configKey, DdcConfigApplier applier) {
        requireMutable();
        String key = requireKey(configKey, "configKey");
        requireApplier(applier);
        if (exactAppliers.putIfAbsent(key, applier) != null) {
            throw new IllegalArgumentException("DDC exact config applier already registered: " + key);
        }
    }

    /**
     * {@inheritDoc}
     * 中文：注册以点结尾的前缀应用器并拒绝重复前缀。
     * English: Registers a dot-terminated prefix applier and rejects duplicates.
     */
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

    /**
     * {@inheritDoc} 中文：按精确键、最长前缀、回退应用器的顺序解析。 English: Resolves by exact key, longest prefix, then fallback.
     */
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

    /**
     * {@inheritDoc}
     * 中文：仅检查精确键或前缀注册，不把回退应用器视为显式注册。
     * English: Checks only exact or prefix registrations, excluding the fallback.
     */
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

    /**
     * 幂等地冻结注册表并发布不可变查找快照。
     * Idempotently freezes the registry and publishes an immutable lookup snapshot.
     */
    public synchronized void freeze() {
        if (snapshot == null) {
            snapshot = immutableSnapshot();
        }
    }

    /**
     * 判断注册表是否已冻结。
     * Indicates whether the registry is frozen.
     *
     * @return 已冻结时为 {@code true}; {@code true} when frozen
     */
    public boolean frozen() {
        return snapshot != null;
    }

    /**
     * 创建引用当前可变注册映射的临时查找快照。
     * Creates a temporary lookup snapshot referencing the current mutable registration maps.
     *
     * @return 临时查找快照; temporary lookup snapshot
     */
    private Snapshot mutableSnapshot() {
        return new Snapshot(exactAppliers, sortedPrefixes());
    }

    /**
     * 创建复制当前注册内容的不可变查找快照。
     * Creates an immutable lookup snapshot copying current registrations.
     *
     * @return 不可变查找快照; immutable lookup snapshot
     */
    private Snapshot immutableSnapshot() {
        return new Snapshot(Map.copyOf(exactAppliers), List.copyOf(sortedPrefixes()));
    }

    /**
     * 从快照解析显式应用器，未命中时返回回退应用器。
     * Resolves an explicit applier from a snapshot and returns the fallback on a miss.
     *
     * @param current   查找快照; lookup snapshot
     * @param configKey 配置键; configuration key
     * @return 匹配或回退应用器; matching or fallback applier
     */
    private DdcConfigApplier resolve(Snapshot current, String configKey) {
        DdcConfigApplier resolved = current.resolve(configKey);
        return resolved == null ? fallback : resolved;
    }

    /**
     * 生成按前缀长度降序排列的前缀应用器列表。
     * Builds a prefix-applier list sorted by descending prefix length.
     *
     * @return 最长前缀优先的列表; longest-prefix-first list
     */
    private List<PrefixApplier> sortedPrefixes() {
        List<PrefixApplier> prefixes = new ArrayList<>();
        prefixAppliers.forEach((prefix, applier) -> prefixes.add(new PrefixApplier(prefix, applier)));
        prefixes.sort(Comparator.comparingInt((PrefixApplier item) -> item.prefix().length()).reversed());
        return prefixes;
    }

    /**
     * 确保注册表尚未冻结。
     * Ensures that the registry has not been frozen.
     *
     * @throws IllegalStateException 注册表已冻结时抛出; thrown when the registry is frozen
     */
    private void requireMutable() {
        if (snapshot != null) {
            throw new IllegalStateException("DDC config applier registry is frozen");
        }
    }

    /**
     * 校验并规范化非空配置键或前缀。
     * Validates and normalizes a nonblank configuration key or prefix.
     *
     * @param value     待校验值; value to validate
     * @param fieldName 错误信息中的字段名; field name used in error messages
     * @return 去除首尾空白的值; trimmed value
     */
    private String requireKey(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    /**
     * 校验配置应用器非空。
     * Validates that a configuration applier is non-null.
     *
     * @param applier 待校验应用器; applier to validate
     * @return 原应用器; original applier
     */
    private DdcConfigApplier requireApplier(DdcConfigApplier applier) {
        if (applier == null) {
            throw new IllegalArgumentException("applier must not be null");
        }
        return applier;
    }

    /**
     * 保存精确键映射和最长前缀优先列表的查找快照。
     * Lookup snapshot holding exact-key mappings and a longest-prefix-first list.
     *
     * @param exactAppliers  精确键应用器映射; exact-key applier map
     * @param prefixAppliers 前缀应用器列表; prefix applier list
     */
    private record Snapshot(
            Map<String, DdcConfigApplier> exactAppliers,
            List<PrefixApplier> prefixAppliers
    ) {

        /**
         * 先按精确键、再按最长匹配前缀解析显式应用器。
         * Resolves an explicit applier by exact key and then longest matching prefix.
         *
         * @param configKey 配置键; configuration key
         * @return 显式应用器，未命中时为 {@code null}; explicit applier, or {@code null} on a miss
         */
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

    /**
     * 配置键前缀及其应用器。
     * Configuration-key prefix and its applier.
     *
     * @param prefix  配置键前缀; configuration-key prefix
     * @param applier 前缀应用器; prefix applier
     */
    private record PrefixApplier(String prefix, DdcConfigApplier applier) {
    }
}
