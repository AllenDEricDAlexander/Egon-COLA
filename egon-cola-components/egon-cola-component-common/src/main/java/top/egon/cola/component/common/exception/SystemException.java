package top.egon.cola.component.common.exception;

/**
 * 系统不可预期异常，通常需要记录日志并排查基础设施或程序错误。
 */
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public SystemException(String message) {
        this(ErrorCodes.SYSTEM_ERROR, message);
    }

    public SystemException(String code, String message) {
        super(message);
        this.code = code;
    }

    public SystemException(String message, Throwable cause) {
        this(ErrorCodes.SYSTEM_ERROR, message, cause);
    }

    public SystemException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
