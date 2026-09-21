package top.egon.cola.archetype.source.serviceopen.domain.course.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Course schedule lifecycle values with an explicit, stable code. */
public enum CourseScheduleStatus implements EgonEnum {

    SCHEDULED(0, "schedule is planned"),
    CANCELLED(1, "schedule is cancelled");

    private final int code;

    private final String message;

    CourseScheduleStatus(int code, String message) {
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
