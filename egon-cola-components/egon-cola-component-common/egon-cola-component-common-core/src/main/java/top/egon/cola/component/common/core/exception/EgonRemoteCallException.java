package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for remote call failures.
 */
public class EgonRemoteCallException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonRemoteCallException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonRemoteCallException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonRemoteCallException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
        super(errorStatus, retryable, cause);
    }

    public EgonRemoteCallException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonRemoteCallException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }

    public EgonRemoteCallException(int code, String status, String message, boolean retryable, Throwable cause) {
        super(code, status, message, retryable, cause);
    }
}
