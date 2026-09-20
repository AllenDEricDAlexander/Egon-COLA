package top.egon.cola.component.bytecode.api.architecture;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum ArchitectureSeverity implements EgonEnum {
    ERROR(0, "ERROR"),
    WARNING(1, "WARNING"),
    INFO(2, "INFO");

    private final int code;
    private final String message;

    ArchitectureSeverity(int code, String message) {
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
