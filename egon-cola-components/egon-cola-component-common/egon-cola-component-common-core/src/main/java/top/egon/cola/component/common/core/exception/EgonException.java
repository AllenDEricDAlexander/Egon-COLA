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

    public EgonException(ErrorStatus errorStatus) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), null);
    }

    public EgonException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), cause);
    }

    public EgonException(int code, String status, String message) {
        this(code, status, message, null);
    }

    public EgonException(int code, String status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }
}
