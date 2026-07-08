package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for unauthenticated access.
 */
public class EgonUnauthorizedException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonUnauthorizedException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonUnauthorizedException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonUnauthorizedException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonUnauthorizedException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
