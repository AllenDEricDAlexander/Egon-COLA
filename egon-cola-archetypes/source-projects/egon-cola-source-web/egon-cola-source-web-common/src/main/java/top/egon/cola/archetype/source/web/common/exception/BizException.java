package top.egon.cola.archetype.source.web.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;

/**
 * Business failure carrying a caller supplied stable String wire code. The code stays published
 * through {@link #getStatus()}; the inherited {@code getCode()} only classifies the failure family.
 */
public class BizException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BizException(String code, String message) {
        super(ResultCode.BUSINESS_ERROR.getCode(), code, message);
    }

    public BizException(String code, String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), code, message, cause);
    }

    public String code() {
        return getStatus();
    }
}
