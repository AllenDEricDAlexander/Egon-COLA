package top.egon.cola.component.common.id.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

/**
 * Signals that a thread was interrupted while waiting for a safe Snowflake
 * timestamp. The interrupted status remains set when this exception is thrown.
 */
public final class IdGenerationInterruptedException extends CommonException {

    /**
     * Creates an interrupted generation failure.
     *
     * @param machineId generator machine identifier
     */
    public IdGenerationInterruptedException(int machineId) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(),
                "Interrupted while waiting to generate Snowflake ID: machineId=" + machineId);
    }
}
