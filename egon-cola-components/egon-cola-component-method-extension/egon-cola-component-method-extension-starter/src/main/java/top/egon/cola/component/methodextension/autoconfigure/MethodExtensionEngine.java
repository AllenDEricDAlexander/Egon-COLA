package top.egon.cola.component.methodextension.autoconfigure;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum MethodExtensionEngine implements EgonEnum {
    AOP(0, "AOP"),
    AGENT(1, "AGENT"),
    DISABLED(2, "DISABLED");

    private final int code;
    private final String message;

    MethodExtensionEngine(int code, String message) {
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
