package top.egon.cola.component.accessguard.core.failure;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum FailurePolicy implements EgonEnum {
    FAIL_OPEN(0, "FAIL_OPEN"),
    FAIL_CLOSED(1, "FAIL_CLOSED"),
    LOCAL_FALLBACK(2, "LOCAL_FALLBACK");

    private final int code;

    private final String message;

    FailurePolicy(int code, String message) {
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
