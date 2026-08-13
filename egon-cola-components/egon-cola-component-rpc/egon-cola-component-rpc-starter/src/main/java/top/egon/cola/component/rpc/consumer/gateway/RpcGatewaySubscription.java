package top.egon.cola.component.rpc.consumer.gateway;

/**
 * 用于停止一次实时 RPC Gateway 目录订阅的句柄。
 *
 * <p>Handle used to stop one live RPC Gateway directory subscription.
 */
@FunctionalInterface
public interface RpcGatewaySubscription extends AutoCloseable {

    /**
     * 关闭订阅并释放监听资源。
     *
     * <p>Closes the subscription and releases listener resources.
     */
    @Override
    void close();
}
