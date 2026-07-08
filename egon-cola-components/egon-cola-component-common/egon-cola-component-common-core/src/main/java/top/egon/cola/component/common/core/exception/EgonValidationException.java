package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for validation failures.
 */
public class EgonValidationException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonValidationException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonValidationException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonValidationException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonValidationException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
