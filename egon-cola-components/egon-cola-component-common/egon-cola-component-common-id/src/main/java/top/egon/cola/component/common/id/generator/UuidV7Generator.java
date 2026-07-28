package top.egon.cola.component.common.id.generator;

import top.egon.cola.component.common.id.uuid.UuidV7;

/**
 * Compatibility strategy that returns canonical UUIDv7 strings.
 *
 * @deprecated use {@code SnowflakeIdGenerator} through {@link LongIdGenerator}
 * for new database primary keys; this compatibility strategy will be removed
 * in the next major version
 */
@Deprecated(since = "5.3.1", forRemoval = true)
@SuppressWarnings("removal")
public class UuidV7Generator implements IdGenerator {

    /**
     * Generates a canonical UUIDv7 string.
     *
     * @return canonical 36-character UUIDv7 string
     */
    @Override
    public String nextId() {
        return UuidV7.string();
    }
}
