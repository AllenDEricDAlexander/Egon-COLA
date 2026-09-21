package top.egon.cola.archetype.source.web.domain.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Permission lifecycle values with an explicit, stable code. */
public enum PermissionStatus implements EgonEnum {

    ACTIVE(0, "permission is active"),
    INACTIVE(1, "permission is inactive");

    private final int code;

    private final String message;

    PermissionStatus(int code, String message) {
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
