package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for expected business rule failures.
 */
public class EgonBusinessException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonBusinessException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonBusinessException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonBusinessException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonBusinessException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
