package top.egon.cola.component.methodextension.autoconfigure;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum MethodExtensionNotReadyPolicy implements EgonEnum {
    PROCEED(0, "PROCEED"),
    REJECT(1, "REJECT"),
    FAIL(2, "FAIL");

    private final int code;
    private final String message;

    MethodExtensionNotReadyPolicy(int code, String message) {
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
