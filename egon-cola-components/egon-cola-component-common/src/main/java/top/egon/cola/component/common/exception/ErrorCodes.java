package top.egon.cola.component.common.exception;

/**
 * common 组件默认错误码，错误码和默认错误消息成对维护。
 */
public enum ErrorCodes {

    SUCCESS("SUCCESS", "处理成功"),

    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败"),

    SYSTEM_ERROR("SYSTEM_ERROR", "系统处理失败"),

    PARAM_ERROR("PARAM_ERROR", "参数错误");

    private final String code;

    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
