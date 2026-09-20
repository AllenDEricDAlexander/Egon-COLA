package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum GuardOutcomeType implements EgonEnum {
    ALLOWED(0, "ALLOWED"),
    REJECTED(1, "REJECTED"),
    DEGRADED(2, "DEGRADED"),
    FAILED(3, "FAILED");

    private final int code;

    private final String message;

    GuardOutcomeType(int code, String message) {
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
