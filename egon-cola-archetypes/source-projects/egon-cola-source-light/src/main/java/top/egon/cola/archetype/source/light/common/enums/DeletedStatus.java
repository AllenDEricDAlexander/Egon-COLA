package top.egon.cola.archetype.source.light.common.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Logical deletion flag shared by every persistence object. */
public enum DeletedStatus implements EgonEnum {

    NOT_DELETED(0, "NOT_DELETED"),
    DELETED(1, "DELETED");

    private final int code;

    private final String message;

    DeletedStatus(int code, String message) {
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
