package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Base runtime exception carrying stable enterprise error status fields.
 */
public class EgonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String status;

    private final boolean retryable;

    public EgonException(ErrorStatus errorStatus) {
        this(errorStatus, false, null);
    }

    public EgonException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus, false, cause);
    }

    public EgonException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), retryable, cause);
    }

    public EgonException(int code, String status, String message) {
        this(code, status, message, false, null);
    }

    public EgonException(int code, String status, String message, Throwable cause) {
        this(code, status, message, false, cause);
    }

    public EgonException(int code, String status, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
