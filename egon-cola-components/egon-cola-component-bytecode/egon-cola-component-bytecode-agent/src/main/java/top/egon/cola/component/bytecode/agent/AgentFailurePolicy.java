package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Locale;

public enum AgentFailurePolicy implements EgonEnum {
    SKIP_CLASS(0, "SKIP_CLASS"),
    DISABLE_FEATURE(1, "DISABLE_FEATURE"),
    MARK_FATAL(2, "MARK_FATAL");

    private final int code;
    private final String message;

    AgentFailurePolicy(int code, String message) {
        this.code = code;
        this.message = message;
    }

    static AgentFailurePolicy parse(String value) {
        if (value == null || value.isBlank()) {
            return SKIP_CLASS;
        }
        return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
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
