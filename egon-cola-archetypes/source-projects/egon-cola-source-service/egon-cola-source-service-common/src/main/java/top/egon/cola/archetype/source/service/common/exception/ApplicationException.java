package top.egon.cola.archetype.source.service.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;

import java.io.Serial;

/**
 * Application use-case rejection. The stable String wire code stays published through
 * {@link #getStatus()}; the inherited numeric code only classifies the failure family.
 */
public final class ApplicationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ApplicationErrorCode error;

    public ApplicationException(ApplicationErrorCode error, String message) {
        this(error, message, null);
    }

    public ApplicationException(ApplicationErrorCode error, String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), error.getStatus(), message, cause);
        this.error = error;
    }

    public ApplicationErrorCode code() {
        return error;
    }
}
