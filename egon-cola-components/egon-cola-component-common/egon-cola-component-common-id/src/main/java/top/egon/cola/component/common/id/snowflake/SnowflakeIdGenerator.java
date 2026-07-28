package top.egon.cola.component.common.id.snowflake;

import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.time.TimeSource;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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

        this.machineId = (int) machineId;
        this.maxClockBackwardMillis = maxClockBackward.toMillis();
        try {
            this.maxClockBackwardNanos = maxClockBackward.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("maxClockBackward is too large: " + maxClockBackward, exception);
        }
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
            long elapsedMillis = elapsedMillis(timeSource.currentTimeMillis());
            long previous = state.get();
            int sequence = nextSequence(previous, elapsedMillis);
            long candidate = SnowflakeIdLayout.packState(elapsedMillis, sequence);
            if (state.compareAndSet(previous, candidate)) {
                return SnowflakeIdLayout.compose(elapsedMillis, machineId, sequence);
            }
        }
    }

    private int nextSequence(long previous, long elapsedMillis) {
        if (previous == UNINITIALIZED_STATE) {
            return elapsedMillis == 0L && machineId == 0 ? 1 : 0;
        }

        long lastElapsedMillis = SnowflakeIdLayout.stateElapsedMillis(previous);
        if (elapsedMillis < lastElapsedMillis) {
            throw new IllegalStateException("Clock moved backward");
        }
        if (elapsedMillis > lastElapsedMillis) {
            return 0;
        }

        int lastSequence = SnowflakeIdLayout.stateSequence(previous);
        if (lastSequence == SnowflakeIdLayout.MAX_SEQUENCE) {
            throw new IllegalStateException("Sequence exhausted for current millisecond");
        }
        return lastSequence + 1;
    }

    private long elapsedMillis(long currentTimeMillis) {
        long elapsedMillis = currentTimeMillis - SnowflakeIdLayout.EPOCH_MILLIS;
        if (elapsedMillis < 0L || elapsedMillis > SnowflakeIdLayout.MAX_ELAPSED_MILLIS) {
            throw new IllegalStateException("Current time is outside the Snowflake timestamp range: "
                    + currentTimeMillis);
        }
        return elapsedMillis;
    }
}
