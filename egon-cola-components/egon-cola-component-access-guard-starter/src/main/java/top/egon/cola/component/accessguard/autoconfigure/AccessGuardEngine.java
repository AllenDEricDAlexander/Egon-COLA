package top.egon.cola.component.accessguard.autoconfigure;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum AccessGuardEngine implements EgonEnum {
    AOP(0, "AOP"),
    DISABLED(1, "DISABLED");

    private final int code;

    private final String message;

    AccessGuardEngine(int code, String message) {
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
