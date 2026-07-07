package top.egon.cola.component.common.id.generator;

import top.egon.cola.component.common.id.uuid.UuidV7;

/**
 * UUIDv7 string ID generator.
 */
public class UuidV7Generator implements IdGenerator {

    @Override
    public String nextId() {
        return UuidV7.string();
    }
}
