package top.egon.cola.component.common.id.snowflake;

import top.egon.cola.component.common.id.exception.ClockMovedBackwardException;
import top.egon.cola.component.common.id.exception.IdGenerationInterruptedException;
import top.egon.cola.component.common.id.exception.SnowflakeTimestampOutOfRangeException;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.time.TimeSource;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Stateful, thread-safe generator for the fixed 41/10/12-bit Snowflake layout.
 * IDs produced by one instance are strictly increasing at the successful CAS
 * linearization point. Correctly configured nodes produce globally unique,
 * time-trend-ordered IDs, but cross-node business events are not guaranteed to
 * be globally strictly ordered.
 */
public final class SnowflakeIdGenerator implements LongIdGenerator {

    /** Default maximum tolerated clock rollback. */
    public static final Duration DEFAULT_MAX_CLOCK_BACKWARD = Duration.ofMillis(5);

    private static final long UNINITIALIZED_STATE = -1L;
    private static final long WAIT_PARK_NANOS = 100_000L;
    private static final long CLOCK_WAIT_MARGIN_NANOS = 1_000_000L;

    private final int machineId;
    private final long maxClockBackwardMillis;
    private final long maxClockBackwardNanos;
    private final TimeSource timeSource;
    private final AtomicLong state = new AtomicLong(UNINITIALIZED_STATE);

    /**
     * Creates a generator using the system clock and the default rollback tolerance.
     *
     * @param machineId explicitly assigned machine identifier in the range 0 to 1023
     */
    public SnowflakeIdGenerator(long machineId) {
        this(machineId, DEFAULT_MAX_CLOCK_BACKWARD, System::currentTimeMillis);
    }

    /**
     * Creates a generator using the system clock.
     *
     * @param machineId explicitly assigned machine identifier in the range 0 to 1023
     * @param maxClockBackward maximum clock rollback that may be waited out
     */
    public SnowflakeIdGenerator(long machineId, Duration maxClockBackward) {
        this(machineId, maxClockBackward, System::currentTimeMillis);
    }

    /**
     * Creates a generator with an injectable wall-clock source.
     *
     * @param machineId explicitly assigned machine identifier in the range 0 to 1023
     * @param maxClockBackward maximum clock rollback that may be waited out
     * @param timeSource wall-clock source used for generation
     */
    public SnowflakeIdGenerator(long machineId, Duration maxClockBackward, TimeSource timeSource) {
        if (machineId < 0L || machineId > SnowflakeIdLayout.MAX_MACHINE_ID) {
            throw new IllegalArgumentException("machineId must be between 0 and 1023: " + machineId);
        }
        Objects.requireNonNull(maxClockBackward, "maxClockBackward must not be null");
        if (maxClockBackward.isNegative()) {
            throw new IllegalArgumentException("maxClockBackward must not be negative: " + maxClockBackward);
        }

        try {
            this.maxClockBackwardMillis = maxClockBackward.toMillis();
            this.maxClockBackwardNanos = maxClockBackward.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("maxClockBackward is too large: " + maxClockBackward, exception);
        }
        this.machineId = (int) machineId;
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    /**
     * Returns the next positive ID. The successful state CAS is the operation's
     * linearization point.
     *
     * @return a positive Snowflake ID
     */
    @Override
    public long nextLongId() {
        while (true) {
            long previous = state.get();
            long currentTimeMillis = timeSource.currentTimeMillis();
            long elapsedMillis = elapsedMillis(currentTimeMillis);
            int sequence;
            if (previous == UNINITIALIZED_STATE) {
                sequence = elapsedMillis == 0L && machineId == 0 ? 1 : 0;
            } else {
                long lastElapsedMillis = SnowflakeIdLayout.stateElapsedMillis(previous);
                long lastTimeMillis = SnowflakeIdLayout.EPOCH_MILLIS + lastElapsedMillis;
                if (elapsedMillis < lastElapsedMillis) {
                    waitForClockRecovery(currentTimeMillis, lastTimeMillis);
                    continue;
                }
                if (elapsedMillis > lastElapsedMillis) {
                    sequence = 0;
                } else {
                    int lastSequence = SnowflakeIdLayout.stateSequence(previous);
                    if (lastSequence == SnowflakeIdLayout.MAX_SEQUENCE) {
                        waitForNextMillis(lastTimeMillis);
                        continue;
                    }
                    sequence = lastSequence + 1;
                }
            }
            long candidate = SnowflakeIdLayout.packState(elapsedMillis, sequence);
            if (state.compareAndSet(previous, candidate)) {
                return SnowflakeIdLayout.compose(elapsedMillis, machineId, sequence);
            }
        }
    }

    private long elapsedMillis(long currentTimeMillis) {
        if (currentTimeMillis < SnowflakeIdLayout.EPOCH_MILLIS
                || currentTimeMillis > SnowflakeIdLayout.MAX_TIMESTAMP_MILLIS) {
            throw new SnowflakeTimestampOutOfRangeException(currentTimeMillis,
                    SnowflakeIdLayout.EPOCH_MILLIS, SnowflakeIdLayout.MAX_TIMESTAMP_MILLIS);
        }
        return currentTimeMillis - SnowflakeIdLayout.EPOCH_MILLIS;
    }

    private void waitForClockRecovery(long currentTimeMillis, long lastTimeMillis) {
        long observedTimeMillis = currentTimeMillis;
        long waitBudgetNanos = saturatedAdd(maxClockBackwardNanos, CLOCK_WAIT_MARGIN_NANOS);
        long startedNanos = System.nanoTime();
        while (observedTimeMillis < lastTimeMillis) {
            long backwardMillis = lastTimeMillis - observedTimeMillis;
            if (backwardMillis > maxClockBackwardMillis
                    || System.nanoTime() - startedNanos >= waitBudgetNanos) {
                throw new ClockMovedBackwardException(observedTimeMillis, lastTimeMillis,
                        backwardMillis, machineId);
            }
            checkInterrupted();
            LockSupport.parkNanos(WAIT_PARK_NANOS);
            checkInterrupted();
            observedTimeMillis = timeSource.currentTimeMillis();
        }
    }

    private void waitForNextMillis(long lastTimeMillis) {
        while (true) {
            checkInterrupted();
            long currentTimeMillis = timeSource.currentTimeMillis();
            if (currentTimeMillis > lastTimeMillis) {
                elapsedMillis(currentTimeMillis);
                return;
            }
            if (currentTimeMillis < lastTimeMillis) {
                waitForClockRecovery(currentTimeMillis, lastTimeMillis);
                continue;
            }
            LockSupport.parkNanos(WAIT_PARK_NANOS);
        }
    }

    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new IdGenerationInterruptedException(machineId);
        }
    }

    private long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
