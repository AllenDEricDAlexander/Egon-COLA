package top.egon.cola.component.rpc.consumer.invocation;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Java completion shape; independent from Gateway/Direct reference mode. */
/** Java completion shape; independent from Gateway/Direct reference mode. */
public enum RpcInvocationMode implements EgonEnum {

    BLOCKING(0, "BLOCKING"),

    ASYNC(1, "ASYNC");

    private final int code;

    private final String message;

    RpcInvocationMode(int code, String message) {
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
