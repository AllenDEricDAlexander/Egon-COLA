package top.egon.cola.component.bytecode.maven.config;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum ArchitectureFailurePolicy implements EgonEnum {
    FAIL(0, "FAIL"),
    WARN(1, "WARN"),
    REPORT_ONLY(2, "REPORT_ONLY");

    private final int code;
    private final String message;

    ArchitectureFailurePolicy(int code, String message) {
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
