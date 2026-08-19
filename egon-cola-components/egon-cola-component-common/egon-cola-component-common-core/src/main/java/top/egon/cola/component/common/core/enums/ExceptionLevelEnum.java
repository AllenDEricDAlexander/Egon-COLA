package top.egon.cola.component.common.core.enums;

/**
 * Severity levels supported by business exceptions.
 */
public enum ExceptionLevelEnum implements EgonEnum {

    TRACE(5, "trace"),

    DEBUG(2, "debug"),

    INFO(4, "info"),

    WARN(3, "warn"),

    ERROR(1, "error"),

    FATAL(6, "fatal");

    private final int code;

    private final String message;

    ExceptionLevelEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static ExceptionLevelEnum fromValue(int value) {
        for (ExceptionLevelEnum level : values()) {
            if (level.code == value) {
                return level;
            }
        }
        return null;
    }
}
