package top.egon.cola.component.common.id.uuid;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SuppressWarnings("removal")
class UuidV7Test {

    @Test
    void uuidV7GeneratesVersion7Uuid() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidV7.generate();
        long after = System.currentTimeMillis();
        long encodedMillis = uuid.getMostSignificantBits() >>> 16;

        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
        assertFalse(encodedMillis < before);
        assertFalse(encodedMillis > after);
    }

    @Test
    void generatorReturnsUuidString() {
        IdGenerator generator = new UuidV7Generator();

        String first = generator.nextId();
        String second = generator.nextId();

        assertFalse(first.isBlank());
        assertNotEquals(first, second);
        assertEquals(36, first.length());
    }

    @Test
    void simpleStringRemovesHyphen() {
        String id = UuidV7.simpleString();

        assertEquals(32, id.length());
        assertFalse(id.contains("-"));
    }
}
