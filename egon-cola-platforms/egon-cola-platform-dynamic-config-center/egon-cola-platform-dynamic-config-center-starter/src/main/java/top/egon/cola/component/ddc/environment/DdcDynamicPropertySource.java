package top.egon.cola.component.ddc.environment;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class DdcDynamicPropertySource
        extends EnumerablePropertySource<AtomicReference<DdcDynamicPropertySource.Snapshot>>
        implements OriginLookup<String> {

    public DdcDynamicPropertySource(String name, Snapshot snapshot) {
        super(name, new AtomicReference<>(Objects.requireNonNull(snapshot, "snapshot")));
    }

    @Override
    public Object getProperty(String name) {
        return unwrap(snapshot().rawValues().get(name));
    }

    @Override
    public String[] getPropertyNames() {
        return snapshot().rawValues().keySet().toArray(String[]::new);
    }

    @Override
    public Origin getOrigin(String name) {
        Object value = snapshot().rawValues().get(name);
        return value instanceof OriginTrackedValue trackedValue
                ? trackedValue.getOrigin()
                : null;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    public Snapshot snapshot() {
        return source.get();
    }

    public void replace(Snapshot snapshot) {
        source.set(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public boolean compareAndSet(Snapshot expected, Snapshot update) {
        return source.compareAndSet(
                Objects.requireNonNull(expected, "expected"),
                Objects.requireNonNull(update, "update")
        );
    }

    private Object unwrap(Object value) {
        return value instanceof OriginTrackedValue trackedValue
                ? trackedValue.getValue()
                : value;
    }

    public record Snapshot(
            String resourceName,
            long version,
            String checksum,
            Map<String, Object> rawValues
    ) {

        public Snapshot {
            Objects.requireNonNull(resourceName, "resourceName");
            Objects.requireNonNull(checksum, "checksum");
            Objects.requireNonNull(rawValues, "rawValues");
            rawValues = Collections.unmodifiableMap(
                    new LinkedHashMap<>(rawValues)
            );
        }

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
