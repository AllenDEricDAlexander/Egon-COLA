package top.egon.fable.common.exception;

public class BizException extends RuntimeException {

    private static final String DEFAULT_CODE = "BIZ_ERROR";

    private final String code;

    public BizException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
