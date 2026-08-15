package top.egon.cola.component.rpc.ddc.security;

/**
 * DDC Credential 可授权的稳定 RPC 操作。
 * / Stable RPC operations authorized by a DDC credential.
 */
public enum DdcRpcOperation {
    SDK_REGISTER,
    SDK_HEARTBEAT,
    SDK_OFFLINE,
    CONFIG_PULL,
    PUBLISH_ACK,
    REGISTRY_REGISTER,
    REGISTRY_HEARTBEAT,
    REGISTRY_DEREGISTER,
    REGISTRY_READ,
    MANAGEMENT_CONFIG_READ,
    MANAGEMENT_CONFIG_WRITE,
    MANAGEMENT_PUBLISH,
    MANAGEMENT_TASK_READ,
    MANAGEMENT_TASK_RETRY,
    MANAGEMENT_INSTANCE_READ,
    MANAGEMENT_ADMISSION_REVOKE,
    MANAGEMENT_SCOPE_READ,
    MANAGEMENT_REGISTRY_READ,
    MANAGEMENT_CATALOG_READ;

    /** 是否属于管理面操作。 / Whether this is a management-plane operation. */
    public boolean management() {
        return name().startsWith("MANAGEMENT_");
    }
}
