package top.egon.cola.component.accessguard.core.failure;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum FailurePoint implements EgonEnum {
    KEY_RESOLUTION(0, "KEY_RESOLUTION"),
    DENY_LIST_STORE(1, "DENY_LIST_STORE"),
    ALLOW_LIST_STORE(2, "ALLOW_LIST_STORE"),
    PENALTY_STORE(3, "PENALTY_STORE"),
    RATE_LIMIT_BACKEND(4, "RATE_LIMIT_BACKEND"),
    EXECUTION(5, "EXECUTION"),
    OBSERVABILITY(6, "OBSERVABILITY");

    private final int code;

    private final String message;

    FailurePoint(int code, String message) {
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
