package top.egon.cola.component.common.id.snowflake;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.generator.IdGenerator;

import java.time.Duration;
import java.util.Objects;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

/**
 * Static entry point for Snowflake ID generation.
 *
 * <p>The class is an inconstructible utility: {@link #initialize(Long, Duration)} binds exactly one
 * {@link SnowflakeLongIdGenerator} state engine per process, and {@link #nextLongId()} and
 * {@link #nextId()} delegate to that same engine. Repeated initialization with an identical
 * configuration reuses the bound engine; a different configuration is rejected because it would
 * silently reseed the sequence of a live generator. There is no default machine identifier and no
 * production reset.</p>
 */
public final class SnowflakeIdGenerator {

    /** Default maximum tolerated clock rollback. */
    public static final Duration DEFAULT_MAX_CLOCK_BACKWARD = Duration.ofMillis(5);

    private static final long MIN_MACHINE_ID = 0L;

    private static final long MAX_MACHINE_ID = 1023L;

    private static final Object BIND_MONITOR = new Object();

    private static volatile Binding binding;

    private SnowflakeIdGenerator() {
    }

    /**
     * Binds the process-wide Snowflake engine, or verifies that the already bound engine uses the
     * identical configuration.
     *
     * @param machineId explicitly assigned machine identifier in the range 0 to 1023
     * @param maxClockBackward maximum clock rollback that may be waited out
     * @throws CommonException when the configuration is invalid or conflicts with the bound engine
     */
    public static void initialize(Long machineId, Duration maxClockBackward) {
        if (machineId == null) {
            throw configurationFailure(
                    "egon.cola.component.id.machine-id must be configured when enabled=true");
        }
        if (machineId < MIN_MACHINE_ID || machineId > MAX_MACHINE_ID) {
            throw configurationFailure(
                    "egon.cola.component.id.machine-id must be between 0 and 1023: " + machineId);
        }
        if (maxClockBackward == null) {
            throw configurationFailure(
                    "egon.cola.component.id.max-clock-backward must not be null");
        }
        if (maxClockBackward.isNegative()) {
            throw configurationFailure(
                    "egon.cola.component.id.max-clock-backward must not be negative: " + maxClockBackward);
        }

        synchronized (BIND_MONITOR) {
            Binding current = binding;
            if (current != null) {
                if (current.machineId() == machineId && current.maxClockBackward().equals(maxClockBackward)) {
                    return;
                }
                throw configurationFailure("Snowflake ID generation is already initialized with"
                        + " machineId=" + current.machineId() + ", maxClockBackward="
                        + current.maxClockBackward() + "; a live engine cannot be rebound to machineId="
                        + machineId + ", maxClockBackward=" + maxClockBackward);
            }
            binding = new Binding(machineId, maxClockBackward,
                    new SnowflakeLongIdGenerator(machineId, maxClockBackward));
        }
    }

    /**
     * Returns the next positive ID from the bound engine.
     *
     * @return a positive Snowflake ID
     * @throws CommonException when the engine has not been initialized
     */
    public static long nextLongId() {
        return boundEngine().nextLongId();
    }

    /**
     * Returns the decimal representation of the next positive ID from the bound engine.
     *
     * @return a positive Snowflake ID as a decimal string
     * @throws CommonException when the engine has not been initialized
     */
    public static String nextId() {
        return boundEngine().nextId();
    }

    private static SnowflakeLongIdGenerator boundEngine() {
        Binding current = binding;
        if (current == null) {
            throw configurationFailure("Snowflake ID generation is not initialized; configure"
                    + " egon.cola.component.id.machine-id or call SnowflakeIdGenerator.initialize"
                    + " before generating an " + IdGenerator.class.getSimpleName());
        }
        return current.engine();
    }

    private static CommonException configurationFailure(String message) {
        return new CommonException(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(),
                message);
    }

    private record Binding(long machineId, Duration maxClockBackward, SnowflakeLongIdGenerator engine) {

        private Binding {
            Objects.requireNonNull(engine, "engine must not be null");
        }
    }
}
