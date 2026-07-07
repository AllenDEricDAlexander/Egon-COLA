package top.egon.cola.component.common.id.generator;

/**
 * Strategy contract for string ID generation.
 */
public interface IdGenerator {

    String nextId();
}
