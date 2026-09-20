package top.egon.cola.component.rpc.contract.descriptor;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum RpcType implements EgonEnum {

    UNARY(0, "UNARY");

    private final int code;

    private final String message;

    RpcType(int code, String message) {
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
