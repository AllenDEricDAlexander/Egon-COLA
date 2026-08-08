package top.egon.cola.component.ddc.refresh;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 描述一次 DDC YAML 配置应用结果的不可变事件。
 * Immutable event describing the result of applying a DDC YAML configuration.
 *
 * @param resourceName        配置资源名称; configuration resource name
 * @param version             配置版本; configuration version
 * @param checksum            配置资源校验和; configuration resource checksum
 * @param changedKeys         所有发生有效变化的配置键; all effectively changed configuration keys
 * @param addedKeys           新增的配置键; added configuration keys
 * @param updatedKeys         已更新的配置键; updated configuration keys
 * @param removedKeys         已移除的配置键; removed configuration keys
 * @param refreshedKeys       已在运行时完成刷新的配置键; configuration keys refreshed at runtime
 * @param restartRequiredKeys 需要重启才能生效的配置键; configuration keys requiring restart to take effect
 * @param changeId            触发本次变化的发布标识; publication identifier that triggered this change
 */
public record DdcConfigurationChangedEvent(
        String resourceName,
        long version,
        String checksum,
        Set<String> changedKeys,
        Set<String> addedKeys,
        Set<String> updatedKeys,
        Set<String> removedKeys,
        Set<String> refreshedKeys,
        Set<String> restartRequiredKeys,
        String changeId
) {

    /**
     * 创建事件并将所有键集合转换为保持迭代顺序的不可变副本。
     * Creates the event and converts every key set into an immutable copy preserving iteration order.
     */
    public DdcConfigurationChangedEvent {
        changedKeys = immutable(changedKeys);
        addedKeys = immutable(addedKeys);
        updatedKeys = immutable(updatedKeys);
        removedKeys = immutable(removedKeys);
        refreshedKeys = immutable(refreshedKeys);
        restartRequiredKeys = immutable(restartRequiredKeys);
    }

    /**
     * 创建保持原迭代顺序的不可变集合副本。
     * Creates an immutable set copy that preserves the original iteration order.
     *
     * @param values 待复制的值集合; values to copy
     * @return 不可变且去重后的集合; immutable, deduplicated set
     */
    private static Set<String> immutable(Set<String> values) {
        return Set.copyOf(new LinkedHashSet<>(values));
    }
}
