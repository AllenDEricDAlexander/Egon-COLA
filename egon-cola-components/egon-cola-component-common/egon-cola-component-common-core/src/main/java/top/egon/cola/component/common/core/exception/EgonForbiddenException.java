package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for forbidden access.
 */
public class EgonForbiddenException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonForbiddenException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonForbiddenException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonForbiddenException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonForbiddenException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
