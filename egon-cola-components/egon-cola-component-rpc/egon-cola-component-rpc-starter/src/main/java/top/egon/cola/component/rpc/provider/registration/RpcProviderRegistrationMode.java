package top.egon.cola.component.rpc.provider.registration;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * 控制 RPC Provider 是否必须向注册中心发布租约。
 *
 * <p>Controls whether an RPC Provider must publish leases to a registry.
 */
public enum RpcProviderRegistrationMode implements EgonEnum {

    REQUIRED(0, "REQUIRED"),

    DISABLED(1, "DISABLED");

    private final int code;

    private final String message;

    RpcProviderRegistrationMode(int code, String message) {
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
