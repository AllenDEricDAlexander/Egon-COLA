package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum AgentState implements EgonEnum {
    DISABLED(0, "DISABLED"),
    STARTING(1, "STARTING"),
    ACTIVE(2, "ACTIVE"),
    DEGRADED(3, "DEGRADED"),
    FAILED(4, "FAILED");

    private final int code;
    private final String message;

    AgentState(int code, String message) {
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
