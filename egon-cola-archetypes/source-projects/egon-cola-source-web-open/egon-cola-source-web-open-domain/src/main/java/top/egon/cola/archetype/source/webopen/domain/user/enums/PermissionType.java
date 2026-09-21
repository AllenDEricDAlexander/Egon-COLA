package top.egon.cola.archetype.source.webopen.domain.user.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Permission kinds with an explicit, stable code. */
public enum PermissionType implements EgonEnum {

    API(0, "api permission"),
    MENU(1, "menu permission"),
    ACTION(2, "action permission");

    private final int code;

    private final String message;

    PermissionType(int code, String message) {
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
