package top.egon.cola.component.common.id.uuid;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UuidV7Test {

    @Test
    void uuidV7GeneratesVersion7Uuid() {
        UUID uuid = UuidV7.generate();

        assertEquals(7, uuid.version());
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
