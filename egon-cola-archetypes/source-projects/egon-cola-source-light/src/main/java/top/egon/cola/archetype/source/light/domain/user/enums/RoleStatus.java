package top.egon.cola.archetype.source.light.domain.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Lifecycle of a role. */
public enum RoleStatus implements EgonEnum {

    ACTIVE(0, "ACTIVE"),
    DISABLED(1, "DISABLED"),
    ARCHIVED(2, "ARCHIVED");

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
