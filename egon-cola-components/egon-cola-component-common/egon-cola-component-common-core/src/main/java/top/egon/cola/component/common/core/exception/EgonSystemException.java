package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for unexpected system failures.
 */
public class EgonSystemException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonSystemException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonSystemException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonSystemException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonSystemException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
