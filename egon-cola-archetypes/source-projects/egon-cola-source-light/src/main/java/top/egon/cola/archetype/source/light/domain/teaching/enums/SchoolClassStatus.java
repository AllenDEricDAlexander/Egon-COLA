package top.egon.cola.archetype.source.light.domain.teaching.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Lifecycle of a school class. */
public enum SchoolClassStatus implements EgonEnum {

    ACTIVE(0, "ACTIVE"),
    ARCHIVED(1, "ARCHIVED");

    private final int code;

    private final String message;

    SchoolClassStatus(int code, String message) {
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
