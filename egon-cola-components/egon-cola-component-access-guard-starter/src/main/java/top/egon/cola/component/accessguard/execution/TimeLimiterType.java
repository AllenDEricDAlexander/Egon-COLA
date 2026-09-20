package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum TimeLimiterType implements EgonEnum {
    CALLER_THREAD(0, "CALLER_THREAD"),
    THREAD_POOL(1, "THREAD_POOL"),
    VIRTUAL_THREAD(2, "VIRTUAL_THREAD");

    private final int code;

    private final String message;

    TimeLimiterType(int code, String message) {
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
