package top.egon.cola.component.bytecode.core.enhance;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum EnhancementFeature implements EgonEnum {
    EXECUTOR(0, "EXECUTOR"),
    OBSERVATION(1, "OBSERVATION"),
    METHOD_EXTENSION(2, "METHOD_EXTENSION");

    private final int code;
    private final String message;

    EnhancementFeature(int code, String message) {
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
