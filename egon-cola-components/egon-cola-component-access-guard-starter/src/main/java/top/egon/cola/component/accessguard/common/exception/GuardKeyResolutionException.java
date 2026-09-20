package top.egon.cola.component.accessguard.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * Thrown when a guard key cannot be resolved from the invocation.
 *
 * <p>The original stable key-resolution code remains the exception status, so failure policies and
 * observability keep matching on it.</p>
 */
public final class GuardKeyResolutionException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GuardKeyResolutionException(String code) {
        super(ResultCode.INVALID_PARAMS.getCode(), code,
                "Access Guard key resolution failed: " + code);
    }
}
