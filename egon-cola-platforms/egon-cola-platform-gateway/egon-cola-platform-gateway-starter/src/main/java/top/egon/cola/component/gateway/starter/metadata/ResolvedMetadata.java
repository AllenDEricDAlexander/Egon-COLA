package top.egon.cola.component.gateway.starter.metadata;

import java.util.Objects;

/**
 * A metadata value together with the level that supplied it.
 *
 * <p>封装解析得到的元数据值及其生效来源；值和来源均不能为空。
 *
 * @param value  the resolved value; never null，解析后的值，不能为 null
 * @param source which level won，最终生效的声明层级
 * @param <T>    value type，元数据值类型
 */
public record ResolvedMetadata<T>(T value, MetadataSource source) {

    /**
     * Validates and initializes a resolved metadata value.
     *
     * <p>校验并初始化已解析的元数据值。
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
     * <p>创建一个由显式声明层级提供的元数据结果。
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
     * <p>创建一个标记为组件默认来源的元数据结果。
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
     * <p>判断值是否在某个声明层级中显式提供，而不是使用默认值。
     *
     * @return {@code true} when the source is not {@link MetadataSource#DEFAULT}
     */
    public boolean explicit() {
        return source.explicit();
    }

    /**
     * Transforms the value while preserving its resolution source.
     *
     * <p>转换元数据值，同时保留原解析来源。
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
