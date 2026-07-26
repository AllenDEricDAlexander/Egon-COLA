package top.egon.cola.component.gateway.starter.metadata;

import java.util.Objects;

/**
 * A metadata value together with the level that supplied it.
 *
 * @param value  the resolved value; never null
 * @param source which level won
 * @param <T>    value type
 */
public record ResolvedMetadata<T>(T value, MetadataSource source) {

    public ResolvedMetadata {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(source, "source");
    }

    public static <T> ResolvedMetadata<T> of(T value, MetadataSource source) {
        return new ResolvedMetadata<>(value, source);
    }

    public static <T> ResolvedMetadata<T> byDefault(T value) {
        return new ResolvedMetadata<>(value, MetadataSource.DEFAULT);
    }

    /** Whether the value was explicitly declared somewhere rather than defaulted. */
    public boolean explicit() {
        return source.explicit();
    }

    public <R> ResolvedMetadata<R> map(java.util.function.Function<T, R> mapper) {
        return new ResolvedMetadata<>(mapper.apply(value), source);
    }
}
