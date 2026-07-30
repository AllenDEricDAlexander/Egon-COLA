package top.egon.cola.component.common.id.snowflake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.egon.cola.component.common.id.exception.ClockMovedBackwardException;
import top.egon.cola.component.common.id.exception.IdGenerationInterruptedException;
import top.egon.cola.component.common.id.exception.SnowflakeTimestampOutOfRangeException;
import top.egon.cola.component.common.id.time.TimeSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SnowflakeIdGeneratorTest {

    private static final long TEST_TIME = SnowflakeIdLayout.EPOCH_MILLIS + 1_000;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(3);

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
    void differentMachineIdsDoNotCollideAcrossAFullSequence() {
        Set<Long> firstMachineIds = generateAtFixedTime(1, TEST_TIME, 4_096);
        Set<Long> secondMachineIds = generateAtFixedTime(2, TEST_TIME, 4_096);

        assertEquals(4_096, firstMachineIds.size());
        assertEquals(4_096, secondMachineIds.size());
        assertTrue(Collections.disjoint(firstMachineIds, secondMachineIds));
    }

    @Test
    void independentGeneratorsWithSameMachineIdCanGenerateDuplicates() {
        SnowflakeIdGenerator firstGenerator = generator(17, TEST_TIME);
        SnowflakeIdGenerator secondGenerator = generator(17, TEST_TIME);

        long first = firstGenerator.nextLongId();
        long second = secondGenerator.nextLongId();

        assertEquals(first, second);
        assertEquals(17, SnowflakeIdParser.parse(first).machineId());
        assertEquals(0, SnowflakeIdParser.parse(first).sequence());
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
    void generatesAllSequencesThenWaitsForNextMillisecond() {
        assertTimeoutPreemptively(WAIT_TIMEOUT, () -> {
            ControllableTimeSource timeSource = new ControllableTimeSource(TEST_TIME);
            SnowflakeIdGenerator generator = generator(5, Duration.ofMillis(5), timeSource);
            List<Long> ids = new ArrayList<>(4_096);

            for (int sequence = 0; sequence < 4_096; sequence++) {
                long id = generator.nextLongId();
                SnowflakeId parsed = SnowflakeIdParser.parse(id);
                assertTrue(id > 0L);
                assertEquals(TEST_TIME - SnowflakeIdLayout.EPOCH_MILLIS, parsed.elapsedMillis());
                assertEquals(5, parsed.machineId());
                assertEquals(sequence, parsed.sequence());
                if (sequence > 0) {
                    assertTrue(id > ids.get(sequence - 1));
                }
                ids.add(id);
            }
            assertEquals(4_096, new HashSet<>(ids).size());

            long readsBeforeWait = timeSource.readCount();
            PendingGeneration pending = startVirtualGeneration(generator, "snowflake-sequence-wait");
            try {
                awaitReadCountAtLeast(timeSource, readsBeforeWait + 2L, WAIT_TIMEOUT);
                assertTrue(pending.thread().isAlive(), "4097th generation must wait for time to advance");
                assertNull(pending.result().get());

                timeSource.advanceMillis(1L);
                awaitTermination(pending.thread(), WAIT_TIMEOUT);
            } finally {
                stopIfAlive(pending.thread());
            }

            assertNull(pending.failure().get());
            Long nextResult = pending.result().get();
            assertNotNull(nextResult);
            long nextId = nextResult;
            SnowflakeId next = SnowflakeIdParser.parse(nextId);
            assertTrue(nextId > ids.get(ids.size() - 1));
            assertEquals(TEST_TIME + 1L - SnowflakeIdLayout.EPOCH_MILLIS, next.elapsedMillis());
            assertEquals(0, next.sequence());
        });
    }

    @Test
    void smallClockRollbackWaitsForManualRecovery() {
        assertTimeoutPreemptively(WAIT_TIMEOUT, () -> {
            ControllableTimeSource timeSource = new ControllableTimeSource(TEST_TIME);
            SnowflakeIdGenerator generator = generator(3, Duration.ofSeconds(1), timeSource);
            long first = generator.nextLongId();

            timeSource.setCurrentTimeMillis(TEST_TIME - 3L);
            long readsBeforeWait = timeSource.readCount();
            PendingGeneration pending = startVirtualGeneration(generator, "snowflake-clock-recovery");
            try {
                awaitReadCountAtLeast(timeSource, readsBeforeWait + 2L, WAIT_TIMEOUT);
                assertTrue(pending.thread().isAlive(), "generation must wait while the clock is behind");

                timeSource.setCurrentTimeMillis(TEST_TIME);
                awaitTermination(pending.thread(), WAIT_TIMEOUT);
            } finally {
                stopIfAlive(pending.thread());
            }

            assertNull(pending.failure().get());
            Long secondResult = pending.result().get();
            assertNotNull(secondResult);
            long second = secondResult;
            assertTrue(second > first);
            assertEquals(1, SnowflakeIdParser.parse(second).sequence());
        });
    }

    @Test
    void largeClockRollbackFailsImmediatelyWithDiagnostics() {
        ControllableTimeSource timeSource = new ControllableTimeSource(TEST_TIME);
        SnowflakeIdGenerator generator = generator(9, Duration.ofMillis(5), timeSource);
        generator.nextLongId();
        timeSource.setCurrentTimeMillis(TEST_TIME - 6L);

        ClockMovedBackwardException exception = assertThrows(
                ClockMovedBackwardException.class, generator::nextLongId);

        assertEquals(TEST_TIME - 6L, exception.currentTimeMillis());
        assertEquals(TEST_TIME, exception.lastTimeMillis());
        assertEquals(6L, exception.backwardMillis());
        assertEquals(9, exception.machineId());
        assertTrue(exception.getMessage().contains("machineId=9"));
    }

    @Test
    void stalledSmallRollbackFailsWithinBoundedWait() {
        ControllableTimeSource timeSource = new ControllableTimeSource(TEST_TIME);
        SnowflakeIdGenerator generator = generator(2, Duration.ofMillis(1), timeSource);
        generator.nextLongId();
        timeSource.setCurrentTimeMillis(TEST_TIME - 1L);

        assertTimeoutPreemptively(WAIT_TIMEOUT,
                () -> assertThrows(ClockMovedBackwardException.class, generator::nextLongId));
    }

    @Test
    void interruptedVirtualThreadWaitingForNextMillisecondThrowsDedicatedException() {
        assertTimeoutPreemptively(WAIT_TIMEOUT, () -> {
            ControllableTimeSource timeSource = new ControllableTimeSource(TEST_TIME);
            SnowflakeIdGenerator generator = generator(4, Duration.ofMillis(5), timeSource);
            for (int i = 0; i < 4_096; i++) {
                generator.nextLongId();
            }

            long readsBeforeWait = timeSource.readCount();
            PendingGeneration pending = startVirtualGeneration(generator, "snowflake-interrupt-wait");
            try {
                awaitReadCountAtLeast(timeSource, readsBeforeWait + 2L, WAIT_TIMEOUT);
                assertTrue(pending.thread().isAlive(), "generation must still be waiting");
                pending.thread().interrupt();
                awaitTermination(pending.thread(), WAIT_TIMEOUT);
            } finally {
                stopIfAlive(pending.thread());
            }

            assertNull(pending.result().get());
            assertInstanceOf(IdGenerationInterruptedException.class, pending.failure().get());
            assertTrue(pending.interruptedAtExit().get());
        });
    }

    @Test
    void largeDeterministicBatchIsUniqueAndStrictlyIncreasing() {
        AtomicLong reads = new AtomicLong();
        TimeSource timeSource = () -> TEST_TIME + reads.getAndIncrement() / 2_048;
        SnowflakeIdGenerator generator = generator(11, Duration.ofMillis(5), timeSource);
        List<Long> ids = new ArrayList<>(100_000);

        for (int i = 0; i < 100_000; i++) {
            ids.add(generator.nextLongId());
        }

        assertEquals(100_000, new HashSet<>(ids).size());
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i) > ids.get(i - 1));
        }
    }

    private Set<Long> generateAtFixedTime(long machineId, long currentTimeMillis, int count) {
        SnowflakeIdGenerator generator = generator(machineId, currentTimeMillis);
        Set<Long> ids = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(generator.nextLongId());
        }
        return ids;
    }

    private SnowflakeIdGenerator generator(long machineId, long currentTimeMillis) {
        return generator(machineId, Duration.ofMillis(5), () -> currentTimeMillis);
    }

    private SnowflakeIdGenerator generator(long machineId, Duration maxClockBackward,
                                             TimeSource timeSource) {
        return new SnowflakeIdGenerator(machineId, maxClockBackward, timeSource);
    }

    private PendingGeneration startVirtualGeneration(SnowflakeIdGenerator generator, String threadName) {
        AtomicReference<Long> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interruptedAtExit = new AtomicBoolean();
        Thread thread = Thread.ofVirtual().name(threadName).unstarted(() -> {
            try {
                result.set(generator.nextLongId());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                interruptedAtExit.set(Thread.currentThread().isInterrupted());
            }
        });
        thread.start();
        return new PendingGeneration(thread, result, failure, interruptedAtExit);
    }

    private void awaitReadCountAtLeast(ControllableTimeSource timeSource, long expectedReadCount,
                                       Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (timeSource.readCount() < expectedReadCount && System.nanoTime() < deadline) {
            LockSupport.parkNanos(100_000L);
        }
        assertTrue(timeSource.readCount() >= expectedReadCount,
                "generator did not enter the clock wait loop before the timeout");
    }

    private void awaitTermination(Thread thread, Duration timeout) throws InterruptedException {
        thread.join(timeout.toMillis());
        assertFalse(thread.isAlive(), "virtual thread did not terminate before the timeout");
    }

    private void stopIfAlive(Thread thread) throws InterruptedException {
        if (thread.isAlive()) {
            thread.interrupt();
            thread.join(WAIT_TIMEOUT.toMillis());
        }
    }

    private record PendingGeneration(Thread thread, AtomicReference<Long> result,
                                     AtomicReference<Throwable> failure,
                                     AtomicBoolean interruptedAtExit) {
    }
}
