package top.egon.cola.component.common.id.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

/**
 * Signals that wall-clock time cannot be represented by the fixed 41-bit
 * Snowflake timestamp field.
 */
public final class SnowflakeTimestampOutOfRangeException extends CommonException {

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
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(),
                "Snowflake timestamp is outside the supported range: currentTimeMillis="
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
