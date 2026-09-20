package top.egon.cola.component.accessguard.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * Thrown when a managed Access Guard executor refuses to accept the guarded call.
 */
public final class ExecutorRejectedException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExecutorRejectedException(Throwable cause) {
        super(ResultCode.CONCURRENCY_ERROR.getCode(), ResultCode.CONCURRENCY_ERROR.getStatus(),
                "Access Guard executor rejected the operation", cause);
    }
}
