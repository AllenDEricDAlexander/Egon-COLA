package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for remote call failures.
 */
public class RemoteCallException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RemoteCallException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public RemoteCallException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public RemoteCallException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
        super(errorStatus, retryable, cause);
    }

    public RemoteCallException(int code, String status, String message) {
        super(code, status, message);
    }

    public RemoteCallException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }

    public RemoteCallException(int code, String status, String message, boolean retryable, Throwable cause) {
        super(code, status, message, retryable, cause);
    }
}
