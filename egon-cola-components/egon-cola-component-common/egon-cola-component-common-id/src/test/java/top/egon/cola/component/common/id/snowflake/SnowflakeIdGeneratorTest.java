package top.egon.cola.component.common.id.snowflake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.egon.cola.component.common.id.exception.ClockMovedBackwardException;
import top.egon.cola.component.common.id.exception.IdGenerationInterruptedException;
import top.egon.cola.component.common.id.exception.SnowflakeTimestampOutOfRangeException;
import top.egon.cola.component.common.id.time.TimeSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    private static final long TEST_TIME = SnowflakeIdLayout.EPOCH_MILLIS + 1_000;

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

        assertThrows(SnowflakeTimestampOutOfRangeException.class, generator::nextLongId);
    }

    @Test
    void rejectsTimeAfterTimestampBitsAreExhausted() {
        long exhausted = SnowflakeIdLayout.EPOCH_MILLIS + SnowflakeIdLayout.MAX_ELAPSED_MILLIS + 1;
        SnowflakeIdGenerator generator = generator(1, exhausted);

        assertThrows(SnowflakeTimestampOutOfRangeException.class, generator::nextLongId);
    }

    @Test
    void sequenceExhaustionWaitsForNextMillisecond() {
        AtomicLong reads = new AtomicLong();
        TimeSource timeSource = () -> reads.getAndIncrement() <= 4_096 ? TEST_TIME : TEST_TIME + 1;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(5, Duration.ofMillis(5), timeSource);

        long first = generator.nextLongId();
        long lastInMillisecond = first;
        for (int i = 1; i < 4_096; i++) {
            lastInMillisecond = generator.nextLongId();
        }
        long nextMillisecond = generator.nextLongId();

        assertEquals(0, SnowflakeIdParser.parse(first).sequence());
        assertEquals(4_095, SnowflakeIdParser.parse(lastInMillisecond).sequence());
        assertEquals(0, SnowflakeIdParser.parse(nextMillisecond).sequence());
        assertEquals(1_001, SnowflakeIdParser.parse(nextMillisecond).elapsedMillis());
        assertTrue(nextMillisecond > lastInMillisecond);
    }

    @Test
    void smallClockRollbackWaitsForRecovery() {
        ScriptedTimeSource timeSource = new ScriptedTimeSource(
                TEST_TIME, TEST_TIME - 3, TEST_TIME - 2, TEST_TIME - 1, TEST_TIME);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(3, Duration.ofMillis(5), timeSource);

        long first = generator.nextLongId();
        long second = generator.nextLongId();

        assertTrue(second > first);
        assertEquals(1, SnowflakeIdParser.parse(second).sequence());
    }

    @Test
    void largeClockRollbackFailsImmediatelyWithDiagnostics() {
        ScriptedTimeSource timeSource = new ScriptedTimeSource(TEST_TIME, TEST_TIME - 6);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(9, Duration.ofMillis(5), timeSource);
        generator.nextLongId();

        ClockMovedBackwardException exception = assertThrows(
                ClockMovedBackwardException.class, generator::nextLongId);

        assertEquals(TEST_TIME - 6, exception.currentTimeMillis());
        assertEquals(TEST_TIME, exception.lastTimeMillis());
        assertEquals(6, exception.backwardMillis());
        assertEquals(9, exception.machineId());
        assertTrue(exception.getMessage().contains("machineId=9"));
    }

    @Test
    void stalledSmallRollbackFailsWithinBoundedWait() {
        ScriptedTimeSource timeSource = new ScriptedTimeSource(TEST_TIME, TEST_TIME - 1);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, Duration.ofMillis(1), timeSource);
        generator.nextLongId();

        assertThrows(ClockMovedBackwardException.class, generator::nextLongId);
    }

    @Test
    void interruptedSequenceWaitPreservesInterruptStatus() {
        SnowflakeIdGenerator generator = generator(4, TEST_TIME);
        for (int i = 0; i < 4_096; i++) {
            generator.nextLongId();
        }

        Thread.currentThread().interrupt();
        try {
            assertThrows(IdGenerationInterruptedException.class, generator::nextLongId);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void largeDeterministicBatchIsUniqueAndStrictlyIncreasing() {
        AtomicLong reads = new AtomicLong();
        TimeSource timeSource = () -> TEST_TIME + reads.getAndIncrement() / 2_048;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(11, Duration.ofMillis(5), timeSource);
        List<Long> ids = new ArrayList<>(100_000);

        for (int i = 0; i < 100_000; i++) {
            ids.add(generator.nextLongId());
        }

        assertEquals(100_000, new HashSet<>(ids).size());
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i) > ids.get(i - 1));
        }
    }

    @Test
    void concurrentGenerationIsUniqueAndStrictlyIncreasingWhenSorted() throws Exception {
        int threadCount = 16;
        int idsPerThread = 5_000;
        AtomicLong reads = new AtomicLong();
        TimeSource timeSource = () -> TEST_TIME + reads.getAndIncrement() / 1_024;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(23, Duration.ofMillis(5), timeSource);
        Set<Long> ids = ConcurrentHashMap.newKeySet(threadCount * idsPerThread);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < idsPerThread; i++) {
                        ids.add(generator.nextLongId());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(threadCount * idsPerThread, ids.size());
        List<Long> sorted = ids.stream().sorted().toList();
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i) > sorted.get(i - 1));
        }
    }

    private SnowflakeIdGenerator generator(long machineId, long currentTimeMillis) {
        return new SnowflakeIdGenerator(machineId, Duration.ofMillis(5), () -> currentTimeMillis);
    }

    private static final class ScriptedTimeSource implements TimeSource {

        private final long[] values;
        private final AtomicLong index = new AtomicLong();

        private ScriptedTimeSource(long... values) {
            this.values = values.clone();
        }

        @Override
        public long currentTimeMillis() {
            int current = (int) Math.min(index.getAndIncrement(), values.length - 1L);
            return values[current];
        }
    }
}
