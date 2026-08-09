package top.egon.cola.component.rpc.provider;

/**
 * 控制 RPC Provider 是否必须向注册中心发布租约。
 *
 * <p>Controls whether an RPC Provider must publish leases to a registry.
 */
public enum RpcProviderRegistrationMode {
    REQUIRED,
    DISABLED
}
