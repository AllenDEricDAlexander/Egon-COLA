package top.egon.cola.archetype.source.service.common.exception;

import top.egon.cola.archetype.source.service.common.enums.EvaluationError;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;

/**
 * Evaluation business failure. The stable String wire code stays published through
 * {@link #getStatus()} while {@link #error()} keeps the typed domain code.
 */
public class EvaluationBizException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final EvaluationError error;

    public EvaluationBizException(EvaluationError error, String message) {
        this(error, message, null);
    }

    public EvaluationBizException(EvaluationError error, String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), error.getStatus(), message, cause);
        this.error = error;
    }

    public EvaluationError error() {
        return error;
    }
}
