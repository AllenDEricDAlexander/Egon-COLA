package ${package}.common.exceptions;

public class BaseBusinessException extends RuntimeException {
    private final String code;

    public BaseBusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BaseBusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
