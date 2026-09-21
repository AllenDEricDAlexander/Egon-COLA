package top.egon.cola.archetype.source.web.domain.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Role lifecycle values with an explicit, stable code. */
public enum RoleStatus implements EgonEnum {

    ACTIVE(0, "role is active"),
    ARCHIVED(1, "role is archived");

    private final int code;

    private final String message;

    RoleStatus(int code, String message) {
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
