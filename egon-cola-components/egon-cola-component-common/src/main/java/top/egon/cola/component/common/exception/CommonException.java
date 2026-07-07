package top.egon.cola.component.common.exception;

import java.io.Serial;

/**
 * common 异常基类，统一保留错误码和错误消息。
 */
public abstract class CommonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String errorMessage;

    protected CommonException(int code, String message) {
        super(message);
        this.code = code;
        this.errorMessage = message;
    }

    protected CommonException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorMessage = message;
    }

    public int getCode() {
        return code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
