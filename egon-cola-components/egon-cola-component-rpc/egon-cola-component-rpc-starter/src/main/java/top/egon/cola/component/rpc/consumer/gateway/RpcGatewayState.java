package top.egon.cola.component.rpc.consumer.gateway;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum RpcGatewayState implements EgonEnum {

    STARTING(0, "STARTING"),

    READY(1, "READY"),

    UNAVAILABLE(2, "UNAVAILABLE"),

    AMBIGUOUS(3, "AMBIGUOUS"),

    STOPPED(4, "STOPPED");

    private final int code;

    private final String message;

    RpcGatewayState(int code, String message) {
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
