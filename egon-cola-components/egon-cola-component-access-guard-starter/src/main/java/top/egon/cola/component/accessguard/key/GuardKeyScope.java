package top.egon.cola.component.accessguard.key;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum GuardKeyScope implements EgonEnum {
    KEY(0, "KEY"),
    GLOBAL(1, "GLOBAL");

    private final int code;

    private final String message;

    GuardKeyScope(int code, String message) {
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
