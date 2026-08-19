package top.egon.cola.component.common.core.enums;

/**
 * Severity levels supported by business exceptions.
 */
public enum ExceptionLevelEnum {

    ERROR(1, "error"),

    DEBUG(2, "debug"),

    WARN(3, "warn"),

    INFO(4, "info");

    private final int code;

    private final String desc;

    ExceptionLevelEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
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
