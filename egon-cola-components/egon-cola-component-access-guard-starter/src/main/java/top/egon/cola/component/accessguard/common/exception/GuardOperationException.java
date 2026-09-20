package top.egon.cola.component.accessguard.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * Marks a business throwable that crossed a managed executor boundary.
 *
 * <p>Both time limiters wrap the worker failure in this type and unwrap exactly one level, so the
 * original throwable type reaches the caller unchanged. The shared type replaces the two identical
 * private wrappers that previously existed per limiter.</p>
 */
public final class GuardOperationException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GuardOperationException(Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(),
                String.valueOf(cause), cause);
    }
}
