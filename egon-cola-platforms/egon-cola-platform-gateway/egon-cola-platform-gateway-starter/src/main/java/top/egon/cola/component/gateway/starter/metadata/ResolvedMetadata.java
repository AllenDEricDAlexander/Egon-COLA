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

    /**
     * Validates and initializes a resolved metadata value.
     *
     * @throws NullPointerException if {@code value} or {@code source} is
     *         {@code null}
     */
    public ResolvedMetadata {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(source, "source");
    }

    /**
     * Creates a value resolved from an explicit declaration source.
     *
     * @param value resolved metadata value
     * @param source declaration level that supplied the value
     * @param <T> metadata value type
     * @return the resolved metadata
     * @throws NullPointerException if {@code value} or {@code source} is
     *         {@code null}
     */
    public static <T> ResolvedMetadata<T> of(T value, MetadataSource source) {
        return new ResolvedMetadata<>(value, source);
    }

    /**
     * Creates a value supplied by the component default.
     *
     * @param value default metadata value
     * @param <T> metadata value type
     * @return the defaulted metadata
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <T> ResolvedMetadata<T> byDefault(T value) {
        return new ResolvedMetadata<>(value, MetadataSource.DEFAULT);
    }

    /**
     * Tests whether the value was explicitly declared somewhere rather than
     * defaulted.
     *
     * @return {@code true} when the source is not {@link MetadataSource#DEFAULT}
     */
    public boolean explicit() {
        return source.explicit();
    }

    /**
     * Transforms the value while preserving its resolution source.
     *
     * @param mapper value transformation
     * @param <R> transformed value type
     * @return transformed metadata with the same source
     * @throws NullPointerException if {@code mapper} is {@code null} or returns
     *         {@code null}
     */
    public <R> ResolvedMetadata<R> map(java.util.function.Function<T, R> mapper) {
        return new ResolvedMetadata<>(mapper.apply(value), source);
    }
}
