package top.egon.cola.component.common.id.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

/**
 * Signals that the wall clock is behind the last timestamp used by a Snowflake
 * generator and cannot be recovered safely within the configured tolerance.
 */
public final class ClockMovedBackwardException extends CommonException {

    private final long currentTimeMillis;
    private final long lastTimeMillis;
    private final long backwardMillis;
    private final int machineId;

    /**
     * Creates a clock rollback failure with complete generation diagnostics.
     *
     * @param currentTimeMillis observed wall-clock time
     * @param lastTimeMillis last wall-clock time used by the generator
     * @param backwardMillis rollback distance in milliseconds
     * @param machineId generator machine identifier
     */
    public ClockMovedBackwardException(long currentTimeMillis, long lastTimeMillis,
                                       long backwardMillis, int machineId) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(),
                "Clock moved backward: currentTimeMillis=" + currentTimeMillis
                        + ", lastTimeMillis=" + lastTimeMillis
                        + ", backwardMillis=" + backwardMillis
                        + ", machineId=" + machineId);
        this.currentTimeMillis = currentTimeMillis;
        this.lastTimeMillis = lastTimeMillis;
        this.backwardMillis = backwardMillis;
        this.machineId = machineId;
    }

    /** @return the observed wall-clock time */
    public long currentTimeMillis() {
        return currentTimeMillis;
    }

    /** @return the last wall-clock time used by the generator */
    public long lastTimeMillis() {
        return lastTimeMillis;
    }

    /** @return the rollback distance in milliseconds */
    public long backwardMillis() {
        return backwardMillis;
    }

    /** @return the generator machine identifier */
    public int machineId() {
        return machineId;
    }
}
