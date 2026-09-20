package top.egon.cola.component.bytecode.api.architecture;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum LocationKind implements EgonEnum {
    CLASS(0, "CLASS"),
    FIELD(1, "FIELD"),
    METHOD(2, "METHOD"),
    INSTRUCTION(3, "INSTRUCTION");

    private final int code;
    private final String message;

    LocationKind(int code, String message) {
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
