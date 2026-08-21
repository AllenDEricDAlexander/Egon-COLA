package top.egon.cola.component.rpc.consumer.loadbalance;

import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;

/** Selects one already-discovered endpoint without performing I/O. */
public interface RpcLoadBalancer {

    RpcEndpoint select(RpcLoadBalanceContext context);

    /** Releases an in-flight reservation held by stateful algorithms. */
    default void release(RpcLoadBalanceContext context, RpcEndpoint endpoint) {
        // Stateless algorithms have no reservation to release.
    }

    /** Removes all per-query state for a no-longer-used logical query. */
    default void remove(String queryIdentity) {
        // Stateless algorithms have no per-query state.
    }

    /** Releases all bounded algorithm state. */
    default void close() {
        // Stateless algorithms have no resources to close.
    }
}
