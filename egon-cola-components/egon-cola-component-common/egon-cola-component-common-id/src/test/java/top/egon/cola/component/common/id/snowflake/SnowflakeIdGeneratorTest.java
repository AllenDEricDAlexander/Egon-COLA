package top.egon.cola.component.common.id.snowflake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void generatesStrictlyIncreasingIdsInOneInstance() {
        SnowflakeIdGenerator generator = generator(7, SnowflakeIdLayout.EPOCH_MILLIS + 100);

        long first = generator.nextLongId();
        long second = generator.nextLongId();

        assertTrue(second > first);
        assertEquals(0, SnowflakeIdParser.parse(first).sequence());
        assertEquals(1, SnowflakeIdParser.parse(second).sequence());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1023L})
    void acceptsMachineIdBoundaries(long machineId) {
        SnowflakeIdGenerator generator = generator(machineId, SnowflakeIdLayout.EPOCH_MILLIS + 1);

        assertEquals(machineId, SnowflakeIdParser.parse(generator.nextLongId()).machineId());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 1024L})
    void rejectsMachineIdOutsideTenBits(long machineId) {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(machineId));
    }

    @Test
    void rejectsNegativeClockBackwardDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(1, Duration.ofMillis(-1)));
    }

    @Test
    void differentMachinesDoNotCollideInSameMillisecond() {
        long timestamp = SnowflakeIdLayout.EPOCH_MILLIS + 10;
        long first = generator(1, timestamp).nextLongId();
        long second = generator(2, timestamp).nextLongId();

        assertNotEquals(first, second);
        assertEquals(1, SnowflakeIdParser.parse(first).machineId());
        assertEquals(2, SnowflakeIdParser.parse(second).machineId());
    }

    @Test
    void exactEpochStillGeneratesPositiveId() {
        long id = generator(0, SnowflakeIdLayout.EPOCH_MILLIS).nextLongId();

        assertTrue(id > 0);
        assertEquals(1, SnowflakeIdParser.parse(id).sequence());
    }

    @Test
    void acceptsLastRepresentableMillisecond() {
        long lastTime = SnowflakeIdLayout.EPOCH_MILLIS + SnowflakeIdLayout.MAX_ELAPSED_MILLIS;

        SnowflakeId parsed = SnowflakeIdParser.parse(generator(1023, lastTime).nextLongId());

        assertEquals(SnowflakeIdLayout.MAX_ELAPSED_MILLIS, parsed.elapsedMillis());
        assertEquals(1023, parsed.machineId());
    }

    @Test
    void rejectsTimeBeforeEpoch() {
        SnowflakeIdGenerator generator = generator(1, SnowflakeIdLayout.EPOCH_MILLIS - 1);

        assertThrows(IllegalStateException.class, generator::nextLongId);
    }

    @Test
    void rejectsTimeAfterTimestampBitsAreExhausted() {
        long exhausted = SnowflakeIdLayout.EPOCH_MILLIS + SnowflakeIdLayout.MAX_ELAPSED_MILLIS + 1;
        SnowflakeIdGenerator generator = generator(1, exhausted);

        assertThrows(IllegalStateException.class, generator::nextLongId);
    }

    private SnowflakeIdGenerator generator(long machineId, long currentTimeMillis) {
        return new SnowflakeIdGenerator(machineId, Duration.ofMillis(5), () -> currentTimeMillis);
    }
}
