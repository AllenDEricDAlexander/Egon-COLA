package top.egon.cola.component.rpc.provider.lifecycle;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Explicit Provider lifecycle authority used by availability and shutdown gates. */
public enum RpcProviderRuntimeState implements EgonEnum {

    NEW(0, "NEW"),
    STARTING(1, "STARTING"),
    READY(2, "READY"),
    DEGRADED(3, "DEGRADED"),
    DRAINING(4, "DRAINING"),
    FAILED(5, "FAILED"),
    STOPPED(6, "STOPPED");

    private final int code;

    private final String message;

    RpcProviderRuntimeState(int code, String message) {
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

    public boolean servingNewCalls() {
        return this == READY || this == DEGRADED;
    }
}
