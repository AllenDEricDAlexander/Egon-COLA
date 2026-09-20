package top.egon.cola.component.common.id.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongIdGeneratorTest {

    private static final long FIXED_ID = 9_223_372_036_854L;

    @Test
    void namedGeneratorImplementsBothIndependentOperations() {
        LongIdGenerator generator = new FixedLongIdGenerator();

        assertEquals(FIXED_ID, generator.nextLongId());
        assertEquals("9223372036854", generator.nextId());
    }

    private static final class FixedLongIdGenerator implements LongIdGenerator {

        @Override
        public long nextLongId() {
            return FIXED_ID;
        }

        @Override
        public String nextId() {
            return Long.toString(nextLongId());
        }
    }
}
