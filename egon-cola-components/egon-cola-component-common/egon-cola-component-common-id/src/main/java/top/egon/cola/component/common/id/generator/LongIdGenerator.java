package top.egon.cola.component.common.id.generator;

/**
 * Strategy contract for generators whose native ID representation is a {@code long}.
 */
@FunctionalInterface
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
    default String nextId() {
        return Long.toString(nextLongId());
    }
}
