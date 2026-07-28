package top.egon.cola.component.common.id.generator;

/**
 * Strategy contract for string ID generation.
 */
public interface IdGenerator {

    /**
     * Generates the next ID.
     *
     * @return the generated ID as a string
     */
    String nextId();
}
