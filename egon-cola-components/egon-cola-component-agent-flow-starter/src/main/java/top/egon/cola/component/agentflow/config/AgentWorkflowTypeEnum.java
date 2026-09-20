package top.egon.cola.component.agentflow.config;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Supported Google ADK composite workflow builders. */
public enum AgentWorkflowTypeEnum implements EgonEnum {
    SEQUENTIAL(0, "SEQUENTIAL"),
    PARALLEL(1, "PARALLEL"),
    LOOP(2, "LOOP");

    private final int code;
    private final String message;

    AgentWorkflowTypeEnum(int code, String message) {
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
