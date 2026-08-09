package top.egon.cola.component.ddc.environment;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.model.config.DdcConfigFormat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 以原子快照承载 DDC YAML 的可枚举动态属性源，并保留属性来源信息。 Provides an enumerable dynamic property source backed by atomic DDC YAML snapshots while preserving property origins.
 */
public final class DdcDynamicPropertySource
        extends EnumerablePropertySource<AtomicReference<DdcDynamicPropertySource.Snapshot>>
        implements OriginLookup<String> {

    /**
     * 使用初始快照创建动态属性源。 Creates a dynamic property source with its initial snapshot.
     *
     * @param name     属性源名称。 property-source name
     * @param snapshot 初始不可变快照。 initial immutable snapshot
     * @throws NullPointerException 快照为 {@code null} 时抛出。 thrown when the snapshot is {@code null}
     */
    public DdcDynamicPropertySource(String name, Snapshot snapshot) {
        super(name, new AtomicReference<>(Objects.requireNonNull(snapshot, "snapshot")));
    }

    /**
     * 从当前快照返回解包后的属性值。 Returns an unwrapped property value from the current snapshot.
     *
     * @param name 属性名。 property name
     * @return 属性值，不存在时为 {@code null}。 property value, or {@code null} when absent
     */
    @Override
    @Nullable
    public Object getProperty(String name) {
        return unwrap(snapshot().rawValues().get(name));
    }

    /**
     * 返回当前快照中的全部属性名。 Returns all property names in the current snapshot.
     *
     * @return 当前属性名数组。 current property-name array
     */
    @Override
    public String[] getPropertyNames() {
        return snapshot().rawValues().keySet().toArray(String[]::new);
    }

    /**
     * 返回当前快照中属性携带的 YAML 来源。 Returns the YAML origin carried by a property in the current snapshot.
     *
     * @param name 属性名。 property name
     * @return 属性来源，未跟踪来源时为 {@code null}。 property origin, or {@code null} when origin is not tracked
     */
    @Override
    @Nullable
    public Origin getOrigin(String name) {
        Object value = snapshot().rawValues().get(name);
        return value instanceof OriginTrackedValue trackedValue
                ? trackedValue.getOrigin()
                : null;
    }

    /**
     * 表明属性源可通过替换快照发生变化。 Indicates that this property source can change through snapshot replacement.
     *
     * @return 始终为 {@code false}。 always {@code false}
     */
    @Override
    public boolean isImmutable() {
        return false;
    }

    /**
     * 返回当前原子快照。 Returns the current atomic snapshot.
     *
     * @return 当前快照。 current snapshot
     */
    public Snapshot snapshot() {
        return source.get();
    }

    /**
     * 无条件替换当前快照。 Unconditionally replaces the current snapshot.
     *
     * @param snapshot 新快照。 new snapshot
     * @throws NullPointerException 新快照为 {@code null} 时抛出。 thrown when the new snapshot is {@code null}
     */
    public void replace(Snapshot snapshot) {
        source.set(Objects.requireNonNull(snapshot, "snapshot"));
    }

    /**
     * 仅当当前快照仍为预期对象时执行原子替换。 Atomically replaces the snapshot only when the current snapshot is still the expected object.
     *
     * @param expected 预期当前快照。 expected current snapshot
     * @param update   新快照。 replacement snapshot
     * @return 替换成功时为 {@code true}。 {@code true} when replacement succeeds
     * @throws NullPointerException 任一快照为 {@code null} 时抛出。 thrown when either snapshot is {@code null}
     */
    public boolean compareAndSet(Snapshot expected, Snapshot update) {
        return source.compareAndSet(
                Objects.requireNonNull(expected, "expected"),
                Objects.requireNonNull(update, "update")
        );
    }

    /**
     * 从来源跟踪包装中提取实际属性值。 Extracts the actual property value from an origin-tracking wrapper.
     *
     * @param value 原始属性值。 raw property value
     * @return 解包后的属性值。 unwrapped property value
     */
    private Object unwrap(Object value) {
        return value instanceof OriginTrackedValue trackedValue
                ? trackedValue.getValue()
                : value;
    }

    /**
     * 表示一次远程 YAML 加载得到的不可变版本快照。 Represents an immutable versioned snapshot produced by one remote YAML load.
     *
     * @param resourceName 远程资源名。 remote resource name
     * @param format       配置格式。 configuration format
     * @param version      远程配置版本。 remote configuration version
     * @param checksum     配置资源摘要。 configuration resource checksum
     * @param rawValues    可能包含来源跟踪包装的原始属性映射。 raw property map that may contain origin-tracking wrappers
     */
    public record Snapshot(
            String resourceName,
            DdcConfigFormat format,
            long version,
            String checksum,
            Map<String, Object> rawValues
    ) {

        /**
         * 校验必需字段并复制为保持顺序的不可修改映射。 Validates required fields and copies values into an insertion-ordered unmodifiable map.
         *
         * @param resourceName 远程资源名。 remote resource name
         * @param format       配置格式。 configuration format
         * @param version      远程配置版本。 remote configuration version
         * @param checksum     配置资源摘要。 configuration resource checksum
         * @param rawValues    可能包含来源跟踪包装的原始属性映射。 raw property map that may contain origin-tracking wrappers
         * @throws NullPointerException 资源名、摘要或属性映射为空时抛出。 thrown when resource name, checksum, or property map is null
         */
        public Snapshot {
            Objects.requireNonNull(resourceName, "resourceName");
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(checksum, "checksum");
            Objects.requireNonNull(rawValues, "rawValues");
            rawValues = Collections.unmodifiableMap(
                    new LinkedHashMap<>(rawValues)
            );
        }

        /**
         * 返回去除来源跟踪包装后的不可修改属性映射。 Returns an unmodifiable property map with origin-tracking wrappers removed.
         *
         * @return 解包后的属性值映射。 unwrapped property-value map
         */
        public Map<String, Object> values() {
            Map<String, Object> values = new LinkedHashMap<>();
            rawValues.forEach((key, value) -> values.put(
                    key,
                    value instanceof OriginTrackedValue trackedValue
                            ? trackedValue.getValue()
                            : value
            ));
            return Collections.unmodifiableMap(values);
        }
    }
}
