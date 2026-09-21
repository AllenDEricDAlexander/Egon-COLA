package top.egon.cola.archetype.source.serviceopen.domain.course.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Course lifecycle values with an explicit, stable code. */
public enum CourseStatus implements EgonEnum {

    ACTIVE(0, "course is active"),
    INACTIVE(1, "course is inactive");

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
