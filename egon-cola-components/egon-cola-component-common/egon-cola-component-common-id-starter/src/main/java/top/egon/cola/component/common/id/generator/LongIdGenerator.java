package top.egon.cola.component.common.id.generator;

/**
 * Strategy contract for generators whose native ID representation is a {@code long}.
 *
 * <p>Both operations are abstract on purpose: a named generator states how it produces the numeric
 * ID and its decimal string form, so the strategy cannot be reduced to a single-method lambda.</p>
 */
public interface LongIdGenerator extends IdGenerator {

    /**
     * Generates the next ID as a primitive value.
     *
     * @return the generated ID
     */
    long nextLongId();

    /**
     * Generates the next ID as its decimal string representation.
     *
     * @return the decimal representation of the generated ID
     */
    @Override
    String nextId();
}
