package top.egon.cola.component.common.id.snowflake;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnowflakeIdParserTest {

    @Test
    void parseReturnsTimestampMachineAndSequence() {
        long id = (123L << 22) | (17L << 12) | 4095L;

        SnowflakeId parsed = SnowflakeIdParser.parse(id);

        assertEquals(Instant.parse("2026-01-01T00:00:00.123Z"), parsed.generatedAt());
        assertEquals(123L, parsed.elapsedMillis());
        assertEquals(17, parsed.machineId());
        assertEquals(4095, parsed.sequence());
    }

    @Test
    void parseRejectsNegativeId() {
        assertThrows(IllegalArgumentException.class, () -> SnowflakeIdParser.parse(-1L));
    }

    @Test
    void layoutComposesIdAndRoundTripsGeneratorState() {
        long id = SnowflakeIdLayout.compose(123L, 17, 4095);
        long state = SnowflakeIdLayout.packState(123L, 4095);

        assertEquals((123L << 22) | (17L << 12) | 4095L, id);
        assertEquals(123L, SnowflakeIdLayout.stateElapsedMillis(state));
        assertEquals(4095, SnowflakeIdLayout.stateSequence(state));
    }
}
