package top.egon.cola.archetype.source.web.domain.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** User lifecycle values with an explicit, stable code. */
public enum UserStatus implements EgonEnum {

    ACTIVE(0, "user is active"),
    DISABLED(1, "user is disabled");

    private final int code;

    private final String message;

    UserStatus(int code, String message) {
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
