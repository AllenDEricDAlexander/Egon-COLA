package top.egon.cola.component.bytecode.api.architecture;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum ArchitectureLayer implements EgonEnum {
    DOMAIN(0, "DOMAIN"),
    APPLICATION(1, "APPLICATION"),
    INFRASTRUCTURE(2, "INFRASTRUCTURE"),
    ADAPTER(3, "ADAPTER"),
    FACADE(4, "FACADE"),
    STARTER(5, "STARTER"),
    COMMON(6, "COMMON"),
    UNKNOWN(7, "UNKNOWN");

    private final int code;
    private final String message;

    ArchitectureLayer(int code, String message) {
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
