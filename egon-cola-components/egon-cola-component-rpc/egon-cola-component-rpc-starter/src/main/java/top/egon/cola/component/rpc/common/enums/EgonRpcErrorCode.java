package top.egon.cola.component.rpc.common.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Stable RPC error codes. The numeric values and constant names are part of the
 * component contract and must never be reassigned.
 */
public enum EgonRpcErrorCode implements EgonEnum {

    RPC_INVALID_CONTRACT(0, "RPC_INVALID_CONTRACT"),

    RPC_PROVIDER_START_FAILED(1, "RPC_PROVIDER_START_FAILED"),

    RPC_REGISTRATION_FAILED(2, "RPC_REGISTRATION_FAILED"),

    RPC_YUHENG_UNAVAILABLE(3, "RPC_YUHENG_UNAVAILABLE"),

    RPC_YUHENG_AMBIGUOUS(4, "RPC_YUHENG_AMBIGUOUS"),

    RPC_SERVICE_NOT_FOUND(5, "RPC_SERVICE_NOT_FOUND"),

    RPC_METHOD_NOT_FOUND(6, "RPC_METHOD_NOT_FOUND"),

    RPC_DEADLINE_EXCEEDED(7, "RPC_DEADLINE_EXCEEDED"),

    RPC_CANCELLED(8, "RPC_CANCELLED"),

    RPC_PROVIDER_UNAVAILABLE(9, "RPC_PROVIDER_UNAVAILABLE"),

    RPC_RATE_LIMITED(10, "RPC_RATE_LIMITED"),

    RPC_INVALID_REQUEST(11, "RPC_INVALID_REQUEST"),

    RPC_PROVIDER_REJECTED(12, "RPC_PROVIDER_REJECTED"),

    RPC_INTERNAL(13, "RPC_INTERNAL");

    private final int code;

    private final String message;

    EgonRpcErrorCode(int code, String message) {
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
