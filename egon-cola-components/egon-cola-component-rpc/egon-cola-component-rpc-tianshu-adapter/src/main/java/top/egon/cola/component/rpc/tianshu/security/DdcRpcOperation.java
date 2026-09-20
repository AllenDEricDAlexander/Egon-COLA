package top.egon.cola.component.rpc.tianshu.security;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Tianshu Credential 可授权的稳定 RPC 操作。
 * / Stable RPC operations authorized by a Tianshu credential.
 */
public enum DdcRpcOperation implements EgonEnum {
    SDK_REGISTER(0, "SDK_REGISTER"),
    SDK_HEARTBEAT(1, "SDK_HEARTBEAT"),
    SDK_OFFLINE(2, "SDK_OFFLINE"),
    CONFIG_PULL(3, "CONFIG_PULL"),
    PUBLISH_ACK(4, "PUBLISH_ACK"),
    REGISTRY_REGISTER(5, "REGISTRY_REGISTER"),
    REGISTRY_HEARTBEAT(6, "REGISTRY_HEARTBEAT"),
    REGISTRY_DEREGISTER(7, "REGISTRY_DEREGISTER"),
    REGISTRY_READ(8, "REGISTRY_READ"),
    MANAGEMENT_CONFIG_READ(9, "MANAGEMENT_CONFIG_READ"),
    MANAGEMENT_CONFIG_WRITE(10, "MANAGEMENT_CONFIG_WRITE"),
    MANAGEMENT_PUBLISH(11, "MANAGEMENT_PUBLISH"),
    MANAGEMENT_TASK_READ(12, "MANAGEMENT_TASK_READ"),
    MANAGEMENT_TASK_RETRY(13, "MANAGEMENT_TASK_RETRY"),
    MANAGEMENT_INSTANCE_READ(14, "MANAGEMENT_INSTANCE_READ"),
    MANAGEMENT_ADMISSION_REVOKE(15, "MANAGEMENT_ADMISSION_REVOKE"),
    MANAGEMENT_SCOPE_READ(16, "MANAGEMENT_SCOPE_READ"),
    MANAGEMENT_REGISTRY_READ(17, "MANAGEMENT_REGISTRY_READ"),
    MANAGEMENT_CATALOG_READ(18, "MANAGEMENT_CATALOG_READ");

    private final int code;

    private final String message;

    DdcRpcOperation(int code, String message) {
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

    /** 是否属于管理面操作。 / Whether this is a management-plane operation. */
    public boolean management() {
        return name().startsWith("MANAGEMENT_");
    }
}
