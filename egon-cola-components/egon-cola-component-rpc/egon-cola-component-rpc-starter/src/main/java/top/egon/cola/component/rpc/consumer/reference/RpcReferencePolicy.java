package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalanceKeyResolver;

/** Fully resolved, hot-path-safe common policy for one RPC method. */
public record RpcReferencePolicy(
        long timeoutMs,
        int retries,
        LoadBalance loadBalance,
        FailStrategy failStrategy,
        String fallbackBean,
        RpcLoadBalanceKeyResolver keyResolver
) {

    public RpcReferencePolicy {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("RPC reference timeout must be positive");
        }
        if (retries < 0) {
            throw new IllegalArgumentException("RPC reference retries must not be negative");
        }
        if (loadBalance == null || loadBalance == LoadBalance.INHERIT) {
            throw new IllegalArgumentException("RPC reference load balance must be resolved");
        }
        if (failStrategy == null || failStrategy == FailStrategy.INHERIT) {
            throw new IllegalArgumentException("RPC reference fail strategy must be resolved");
        }
        fallbackBean = fallbackBean == null ? "" : fallbackBean.trim();
        if (failStrategy == FailStrategy.LOCAL_FALLBACK && fallbackBean.isBlank()) {
            throw new IllegalArgumentException(
                    "LOCAL_FALLBACK requires fallbackBean");
        }
        if (failStrategy != FailStrategy.LOCAL_FALLBACK && !fallbackBean.isBlank()) {
            throw new IllegalArgumentException(
                    "fallbackBean is only valid with LOCAL_FALLBACK");
        }
        if (loadBalance == LoadBalance.CONSISTENT_HASH && keyResolver == null) {
            throw new IllegalArgumentException(
                    "CONSISTENT_HASH requires a load-balance key resolver");
        }
        if (loadBalance != LoadBalance.CONSISTENT_HASH && keyResolver != null) {
            throw new IllegalArgumentException(
                    "load-balance key resolver is only valid with CONSISTENT_HASH");
        }
    }
}
