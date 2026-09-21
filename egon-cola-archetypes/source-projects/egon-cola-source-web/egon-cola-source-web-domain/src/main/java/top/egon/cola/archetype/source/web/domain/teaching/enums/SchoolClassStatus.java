package top.egon.cola.archetype.source.web.domain.teaching.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** School class lifecycle values with an explicit, stable code. */
public enum SchoolClassStatus implements EgonEnum {

    ACTIVE(0, "school class is active"),
    ARCHIVED(1, "school class is archived");

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
