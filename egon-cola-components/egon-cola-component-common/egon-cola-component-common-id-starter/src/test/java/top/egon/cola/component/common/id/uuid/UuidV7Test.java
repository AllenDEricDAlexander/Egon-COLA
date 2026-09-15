package top.egon.cola.component.common.id.uuid;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    void simpleStringRemovesHyphen() {
        String id = UuidV7.simpleString();

        assertEquals(32, id.length());
        assertFalse(id.contains("-"));
    }
}
