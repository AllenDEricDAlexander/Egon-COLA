package top.egon.cola.component.accessguard.policy.allow;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum AllowListMode implements EgonEnum {
    GATE(0, "GATE"),
    BYPASS_RATE_LIMIT(1, "BYPASS_RATE_LIMIT"),
    BYPASS_RATE_LIMIT_AND_PENALTY(2, "BYPASS_RATE_LIMIT_AND_PENALTY");

    private final int code;

    private final String message;

    AllowListMode(int code, String message) {
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
