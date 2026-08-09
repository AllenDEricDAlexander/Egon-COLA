package top.egon.cola.component.rpc.provider;

/**
 * 发布 RPC Provider 租约，且不向运行时暴露具体注册中心实现。
 *
 * <p>Publishes RPC Provider leases without exposing any concrete registry
 * implementation.
 */
public interface RpcProviderRegistry {

    /**
     * 注册一个 RPC Provider 服务端点。
     *
     * <p>Registers one RPC Provider service endpoint.
     *
     * @param registration RPC Provider 注册信息 / registration data
     * @return 当前有效租约 / active lease
     */
    RpcProviderLease register(RpcProviderRegistration registration);

    /**
     * 续约指定的完整租约身份。
     *
     * <p>Renews the lease identified by its complete identity.
     *
     * @param lease 完整租约身份 / complete lease identity
     * @return 续约结果 / renewal result
     */
    RpcLeaseOperationResult heartbeat(RpcProviderLeaseIdentity lease);

    /**
     * 注销指定的完整租约身份。
     *
     * <p>Deregisters the lease identified by its complete identity.
     *
     * @param lease 完整租约身份 / complete lease identity
     * @return 注销结果 / deregistration result
     */
    RpcLeaseOperationResult deregister(RpcProviderLeaseIdentity lease);
}
