package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum TimeLimitMode implements EgonEnum {
    DISABLED(0, "DISABLED"),
    OBSERVE_ONLY(1, "OBSERVE_ONLY"),
    ENFORCE(2, "ENFORCE");

    private final int code;

    private final String message;

    TimeLimitMode(int code, String message) {
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
