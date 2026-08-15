package top.egon.cola.component.rpc.consumer.provider;

import java.util.function.Consumer;

/**
 * 提供实时 RPC Provider 快照，且不向消费者暴露服务发现技术。
 *
 * <p>Supplies live RPC Provider snapshots without exposing discovery
 * technology.
 */
@FunctionalInterface
public interface RpcProviderDirectory {

    /**
     * 订阅匹配精确查询条件的 RPC Provider 快照。
     *
     * <p>Subscribes to RPC Provider snapshots matching the exact query.
     *
     * @param query Provider 查询条件 / provider query
     * @param listener 快照监听器 / snapshot listener
     * @return 可关闭的订阅 / closeable subscription
     */
    RpcProviderSubscription subscribe(
            RpcProviderQuery query,
            Consumer<RpcProviderSnapshot> listener
    );
}
