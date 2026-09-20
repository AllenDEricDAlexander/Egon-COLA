package top.egon.cola.component.accessguard.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * Thrown when a guard store or backend cannot complete an operation.
 *
 * <p>The original store code remains the exception status; the numeric code only classifies the
 * failure as a middleware problem, and retryability stays undeclared because the stores never
 * claimed it.</p>
 */
public final class StoreOperationException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public StoreOperationException(String code) {
        super(ResultCode.MIDDLEWARE_ERROR.getCode(), code, code);
    }

    public StoreOperationException(String code, Throwable cause) {
        super(ResultCode.MIDDLEWARE_ERROR.getCode(), code, code, cause);
    }
}
