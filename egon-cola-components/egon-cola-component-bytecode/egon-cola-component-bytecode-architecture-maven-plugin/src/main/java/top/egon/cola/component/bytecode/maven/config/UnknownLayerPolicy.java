package top.egon.cola.component.bytecode.maven.config;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum UnknownLayerPolicy implements EgonEnum {
    IGNORE(0, "IGNORE"),
    WARN(1, "WARN"),
    FAIL(2, "FAIL");

    private final int code;
    private final String message;

    UnknownLayerPolicy(int code, String message) {
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
