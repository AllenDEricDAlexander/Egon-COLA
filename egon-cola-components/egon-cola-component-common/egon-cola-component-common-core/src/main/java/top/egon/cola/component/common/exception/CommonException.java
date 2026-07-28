package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Base runtime exception carrying stable enterprise error status fields.
 */
public class CommonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String status;

    private final boolean retryable;

    public CommonException(ErrorStatus errorStatus) {
        this(errorStatus, false, null);
    }

    public CommonException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus, false, cause);
    }

    public CommonException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), retryable, cause);
    }

    public CommonException(int code, String status, String message) {
        this(code, status, message, false, null);
    }

    public CommonException(int code, String status, String message, Throwable cause) {
        this(code, status, message, false, cause);
    }

    public CommonException(int code, String status, String message, boolean retryable, Throwable cause) {
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
