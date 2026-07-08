package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for missing resources.
 */
public class EgonNotFoundException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonNotFoundException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonNotFoundException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonNotFoundException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonNotFoundException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
