package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum GuardResolution implements EgonEnum {
    NONE(0, "NONE"),
    THROWN(1, "THROWN"),
    FALLBACK(2, "FALLBACK"),
    RETURN_JSON(3, "RETURN_JSON"),
    RETURN_NULL(4, "RETURN_NULL"),
    FAIL_OPEN(5, "FAIL_OPEN"),
    LOCAL_FALLBACK(6, "LOCAL_FALLBACK");

    private final int code;

    private final String message;

    GuardResolution(int code, String message) {
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
}
