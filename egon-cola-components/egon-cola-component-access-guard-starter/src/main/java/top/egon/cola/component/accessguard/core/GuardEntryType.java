package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum GuardEntryType implements EgonEnum {
    AOP(0, "AOP"),
    PROGRAMMATIC(1, "PROGRAMMATIC");

    private final int code;

    private final String message;

    GuardEntryType(int code, String message) {
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
