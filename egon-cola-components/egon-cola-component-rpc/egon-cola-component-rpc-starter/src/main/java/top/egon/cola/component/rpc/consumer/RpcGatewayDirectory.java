package top.egon.cola.component.rpc.consumer;

import java.util.function.Consumer;

/**
 * 提供实时 RPC Gateway 快照，且不向消费者暴露服务发现技术。
 *
 * <p>Supplies live RPC Gateway snapshots without exposing discovery
 * technology.
 */
public interface RpcGatewayDirectory {

    /**
     * 订阅匹配查询条件的 RPC Gateway 快照。
     *
     * <p>Subscribes to RPC Gateway snapshots matching the query.
     *
     * @param query Gateway 查询条件 / gateway query
     * @param listener 快照监听器 / snapshot listener
     * @return 可关闭的订阅 / closeable subscription
     */
    RpcGatewaySubscription subscribe(
            RpcGatewayQuery query,
            Consumer<RpcGatewaySnapshot> listener
    );
}
