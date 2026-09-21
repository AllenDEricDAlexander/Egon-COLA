package top.egon.cola.archetype.source.lightopen.domain.teaching.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Lifecycle of a course. */
public enum CourseStatus implements EgonEnum {

    ACTIVE(0, "ACTIVE"),
    DISABLED(1, "DISABLED");

    private final int code;

    private final String message;

    CourseStatus(int code, String message) {
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
