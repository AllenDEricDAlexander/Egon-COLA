package top.egon.cola.component.gateway.starter.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Resolves a value by walking the declaration chain from most specific to least, recording which
 * level won.
 *
 * <p>Deliberately annotation-agnostic. Callers hand it already-extracted candidates, so this
 * class compiles and tests without the RPC annotations — which are an optional dependency here —
 * and the same chain logic serves RPC and HTTP declarations rather than being written twice.
 *
 * <p>The whole mechanism depends on being able to tell "not set" from "set to a value that
 * happens to equal the default". That is what the sentinel predicates are for: a field defaulting
 * to {@code -1} or {@code INHERIT} means unset, so an explicit {@code 0} at method level still
 * overrides a service-level {@code 5}. With plain defaults that distinction is unrecoverable and
 * inheritance silently stops working.
 *
 * <p>Typical use:
 * <pre>{@code
 * long timeout = MetadataResolver.<Long>chain()
 *         .candidate(MetadataSource.METHOD, methodAnnotation.timeoutMs())
 *         .candidate(MetadataSource.CLASS, serviceAnnotation.timeoutMs())
 *         .candidate(MetadataSource.SERVICE_META, meta.timeoutMs())
 *         .candidate(MetadataSource.CONFIGURATION, properties.getTimeoutMs())
 *         .unsetWhen(value -> value == null || value < 0)
 *         .orDefault(3000L)
 *         .value();
 * }</pre>
 *
 * @param <T> metadata value type
 */
public final class MetadataResolver<T> {

    /**
     * Treats a null, negative number, blank string, empty collection or empty
     * object array as unset.
     */
    public static final Predicate<Object> DEFAULT_SENTINEL = value -> switch (value) {
        case null -> true;
        case Number number -> number.doubleValue() < 0;
        case CharSequence text -> text.isEmpty() || text.toString().isBlank();
        case java.util.Collection<?> collection -> collection.isEmpty();
        case Object[] array -> array.length == 0;
        default -> false;
    };

    /** Candidate declarations accumulated for precedence-based resolution. */
    private final List<Candidate<T>> candidates = new ArrayList<>();

    /** Predicate that identifies candidate values which do not declare metadata. */
    private Predicate<? super T> unset = DEFAULT_SENTINEL;

    /**
     * Creates an empty resolution chain using {@link #DEFAULT_SENTINEL}.
     */
    private MetadataResolver() {
    }

    /**
     * Starts an empty metadata resolution chain.
     *
     * @param <T> metadata value type
     * @return a new resolver using {@link #DEFAULT_SENTINEL}
     */
    public static <T> MetadataResolver<T> chain() {
        return new MetadataResolver<>();
    }

    /**
     * Adds a candidate at the given level.
     *
     * <p>Order of addition does not matter; levels are ranked by {@link MetadataSource}
     * precedence, so a caller cannot accidentally invert the chain by reordering calls.
     *
     * @param source declaration level that supplied the candidate
     * @param value candidate metadata value
     * @return this resolver
     */
    public MetadataResolver<T> candidate(MetadataSource source, T value) {
        candidates.add(new Candidate<>(source, value));
        return this;
    }

    /**
     * Replaces the sentinel test for a type whose unset marker is not the usual one.
     *
     * @param predicate predicate returning {@code true} for an unset value
     * @return this resolver
     */
    public MetadataResolver<T> unsetWhen(Predicate<? super T> predicate) {
        this.unset = predicate;
        return this;
    }

    /**
     * Resolves the highest-precedence set candidate.
     *
     * @param defaultValue value used when every candidate is unset
     * @return the resolved value and its winning source, or
     *         {@link MetadataSource#DEFAULT} when the fallback is used
     */
    public ResolvedMetadata<T> orDefault(T defaultValue) {
        return candidates.stream()
                .filter(candidate -> !unset.test(candidate.value()))
                .min(java.util.Comparator.comparingInt(candidate -> candidate.source().ordinal()))
                .map(candidate -> ResolvedMetadata.of(candidate.value(), candidate.source()))
                .orElseGet(() -> ResolvedMetadata.byDefault(defaultValue));
    }

    /**
     * Candidate value supplied by one declaration level.
     *
     * @param source declaration level that supplied the candidate
     * @param value candidate metadata value
     * @param <T> metadata value type
     */
    private record Candidate<T>(MetadataSource source, T value) {
    }
}
