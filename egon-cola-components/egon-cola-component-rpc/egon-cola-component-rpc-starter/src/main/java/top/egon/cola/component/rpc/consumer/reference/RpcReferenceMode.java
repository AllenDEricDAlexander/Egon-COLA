package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Client-selected transport mode; there is intentionally no AUTO value.
 * {@link #DIRECT} is the default for {@code @EgonRpcReference}; Gateway proxy
 * calls must select {@link #GATEWAY} explicitly.
 */
public enum RpcReferenceMode implements EgonEnum {

    GATEWAY(0, "GATEWAY"),

    DIRECT(1, "DIRECT");

    private final int code;

    private final String message;

    RpcReferenceMode(int code, String message) {
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
