package top.egon.cola.archetype.source.light.facade.teaching.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Course status as published by the teaching facade. */
public enum CourseFacadeStatus implements EgonEnum {

    ACTIVE(0, "ACTIVE"),
    DISABLED(1, "DISABLED"),
    UNKNOWN(2, "UNKNOWN");

    private final int code;

    private final String message;

    CourseFacadeStatus(int code, String message) {
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
