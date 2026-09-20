package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum RejectionMode implements EgonEnum {
    THROW(0, "THROW"),
    FALLBACK(1, "FALLBACK"),
    RETURN_JSON(2, "RETURN_JSON"),
    RETURN_NULL(3, "RETURN_NULL");

    private final int code;

    private final String message;

    RejectionMode(int code, String message) {
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
