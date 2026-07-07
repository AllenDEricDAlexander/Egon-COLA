package top.egon.cola.component.common.exception;

/**
 * 业务可预期异常，调用方可根据错误码进行业务处理。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public BusinessException(String message) {
        this(ErrorCodes.BUSINESS_ERROR, message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        this(ErrorCodes.BUSINESS_ERROR, message, cause);
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
