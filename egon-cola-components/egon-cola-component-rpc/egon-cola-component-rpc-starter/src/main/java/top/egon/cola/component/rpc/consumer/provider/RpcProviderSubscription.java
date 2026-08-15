package top.egon.cola.component.rpc.consumer.provider;

/**
 * 用于停止一次实时 RPC Provider 目录订阅的句柄。
 *
 * <p>Handle used to stop one live RPC Provider directory subscription.
 */
@FunctionalInterface
public interface RpcProviderSubscription extends AutoCloseable {

    /**
     * 关闭订阅并释放监听资源。
     *
     * <p>Closes the subscription and releases listener resources.
     */
    @Override
    void close();
}
