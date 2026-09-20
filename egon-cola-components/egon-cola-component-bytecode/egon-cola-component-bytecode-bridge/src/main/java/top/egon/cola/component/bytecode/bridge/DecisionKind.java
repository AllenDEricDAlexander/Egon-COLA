package top.egon.cola.component.bytecode.bridge;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum DecisionKind implements EgonEnum {
    PROCEED(0, "PROCEED"),
    RETURN_NULL(1, "RETURN_NULL"),
    RETURN_VALUE(2, "RETURN_VALUE"),
    THROW(3, "THROW");

    private final int code;
    private final String message;

    DecisionKind(int code, String message) {
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
