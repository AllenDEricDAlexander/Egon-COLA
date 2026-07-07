package top.egon.cola.component.common.exception;

/**
 * common 组件默认错误码，错误码和默认错误消息成对维护。
 */
public enum ErrorCodes {

    SUCCESS(0, "处理成功"),

    BUSINESS_ERROR(400, "业务处理失败"),

    SYSTEM_ERROR(500, "系统处理失败"),

    PARAM_ERROR(400, "参数错误");

    private final int code;

    private final String message;

    ErrorCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
