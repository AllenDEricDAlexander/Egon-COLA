package top.egon.cola.component.rpc.context;

/**
 * 提供同一进程内所有 RPC 服务共享的物理身份。
 *
 * <p>Supplies the physical identity shared by RPC services in one process.
 */
@FunctionalInterface
public interface RpcProcessIdentityProvider {

    /**
     * 获取当前 RPC 进程身份。
     *
     * <p>Returns the current RPC process identity.
     *
     * @return RPC 进程身份 / RPC process identity
     */
    RpcProcessIdentity identity();
}
