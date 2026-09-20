package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum GuardInvocationKind implements EgonEnum {
    METHOD(0, "METHOD"),
    OPERATION(1, "OPERATION");

    private final int code;

    private final String message;

    GuardInvocationKind(int code, String message) {
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
