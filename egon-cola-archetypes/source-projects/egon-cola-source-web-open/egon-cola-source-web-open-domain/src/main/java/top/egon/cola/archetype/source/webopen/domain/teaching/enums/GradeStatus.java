package top.egon.cola.archetype.source.webopen.domain.teaching.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Grade lifecycle values with an explicit, stable code. */
public enum GradeStatus implements EgonEnum {

    ACTIVE(0, "grade is active"),
    ARCHIVED(1, "grade is archived");

    private final int code;

    private final String message;

    GradeStatus(int code, String message) {
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
