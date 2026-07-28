package top.egon.cola.component.common.id.exception;

/**
 * Signals that wall-clock time cannot be represented by the fixed 41-bit
 * Snowflake timestamp field.
 */
public final class SnowflakeTimestampOutOfRangeException extends IllegalStateException {

    private final long currentTimeMillis;
    private final long minimumTimeMillis;
    private final long maximumTimeMillis;

    /**
     * Creates a timestamp-range failure.
     *
     * @param currentTimeMillis observed wall-clock time
     * @param minimumTimeMillis fixed Snowflake epoch
     * @param maximumTimeMillis last representable wall-clock millisecond
     */
    public SnowflakeTimestampOutOfRangeException(long currentTimeMillis, long minimumTimeMillis,
                                                 long maximumTimeMillis) {
        super("Snowflake timestamp is outside the supported range: currentTimeMillis="
                + currentTimeMillis + ", minimumTimeMillis=" + minimumTimeMillis
                + ", maximumTimeMillis=" + maximumTimeMillis);
        this.currentTimeMillis = currentTimeMillis;
        this.minimumTimeMillis = minimumTimeMillis;
        this.maximumTimeMillis = maximumTimeMillis;
    }

    /** @return the observed wall-clock time */
    public long currentTimeMillis() {
        return currentTimeMillis;
    }

    /** @return the fixed Snowflake epoch */
    public long minimumTimeMillis() {
        return minimumTimeMillis;
    }

    /** @return the last representable wall-clock millisecond */
    public long maximumTimeMillis() {
        return maximumTimeMillis;
    }
}
