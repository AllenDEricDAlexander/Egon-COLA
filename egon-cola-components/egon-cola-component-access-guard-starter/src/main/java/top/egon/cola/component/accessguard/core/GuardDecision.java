package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum GuardDecision implements EgonEnum {
    PASS(0, "PASS"),
    DENY_LIST_HIT(1, "DENY_LIST_HIT"),
    ALLOW_LIST_MISS(2, "ALLOW_LIST_MISS"),
    PENALTY_ACTIVE(3, "PENALTY_ACTIVE"),
    RATE_LIMITED(4, "RATE_LIMITED"),
    KEY_RESOLUTION_FAILED(5, "KEY_RESOLUTION_FAILED"),
    STORE_FAILED(6, "STORE_FAILED"),
    CONFIG_FAILED(7, "CONFIG_FAILED"),
    TIME_LIMIT_EXCEEDED(8, "TIME_LIMIT_EXCEEDED"),
    EXECUTOR_REJECTED(9, "EXECUTOR_REJECTED"),
    BUSINESS_EXCEPTION(10, "BUSINESS_EXCEPTION"),
    CANCELLED(11, "CANCELLED");

    private final int code;

    private final String message;

    GuardDecision(int code, String message) {
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
