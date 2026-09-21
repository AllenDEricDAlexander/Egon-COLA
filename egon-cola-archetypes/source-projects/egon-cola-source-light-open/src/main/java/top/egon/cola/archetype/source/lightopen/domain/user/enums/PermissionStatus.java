package top.egon.cola.archetype.source.lightopen.domain.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Lifecycle of a permission. */
public enum PermissionStatus implements EgonEnum {

    ACTIVE(0, "ACTIVE"),
    DISABLED(1, "DISABLED");

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
