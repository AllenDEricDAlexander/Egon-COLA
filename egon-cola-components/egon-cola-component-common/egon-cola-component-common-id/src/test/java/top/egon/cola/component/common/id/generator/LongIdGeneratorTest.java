package top.egon.cola.component.common.id.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongIdGeneratorTest {

    @Test
    void nextIdReturnsDecimalRepresentationOfLongId() {
        LongIdGenerator generator = () -> 9_223_372_036_854L;

        assertEquals("9223372036854", generator.nextId());
    }
}
