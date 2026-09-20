package top.egon.cola.component.rpc.consumer.lifecycle;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Complete Consumer lifecycle state, including drain and failed startup. */
public enum RpcConsumerRuntimeState implements EgonEnum {

    NEW(0, "NEW"),
    STARTING(1, "STARTING"),
    READY(2, "READY"),
    DEGRADED(3, "DEGRADED"),
    DRAINING(4, "DRAINING"),
    FAILED(5, "FAILED"),
    STOPPED(6, "STOPPED");

    private final int code;

    private final String message;

    RpcConsumerRuntimeState(int code, String message) {
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

    public boolean accepting() {
        return this == READY || this == DEGRADED;
    }
}
