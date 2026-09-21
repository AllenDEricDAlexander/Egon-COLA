package top.egon.cola.archetype.source.lightopen.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;

/**
 * Family-wide business failure. The stable wire code stays a String and is carried by
 * {@link #getStatus()}; the inherited numeric code classifies the failure family.
 */
public class BaseBusinessException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BaseBusinessException(String code, String message) {
        super(ResultCode.BUSINESS_ERROR.getCode(), code, message);
    }

    public BaseBusinessException(String code, String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), code, message, cause);
    }
}
