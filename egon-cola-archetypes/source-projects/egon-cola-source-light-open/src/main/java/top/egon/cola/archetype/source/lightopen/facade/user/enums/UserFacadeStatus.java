package top.egon.cola.archetype.source.lightopen.facade.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** User status as published by the user facade. */
public enum UserFacadeStatus implements EgonEnum {

    ACTIVE(0, "ACTIVE"),
    DISABLED(1, "DISABLED"),
    UNKNOWN(2, "UNKNOWN");

    private final int code;

    private final String message;

    UserFacadeStatus(int code, String message) {
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
