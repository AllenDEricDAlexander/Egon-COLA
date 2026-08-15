package top.egon.cola.component.rpc.consumer.provider;

import java.time.Instant;
import java.util.List;

/**
 * 对当前可用 RPC Provider 端点的一次不可变观测。
 *
 * <p>One immutable observation of the available RPC Provider endpoints.
 */
public record RpcProviderSnapshot(
        long revision,
        Instant observedAt,
        List<RpcProviderEndpoint> endpoints
) {

    public RpcProviderSnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "RPC Provider snapshot revision must not be negative"
            );
        }
        if (observedAt == null) {
            throw new IllegalArgumentException(
                    "RPC Provider snapshot observation time is required"
            );
        }
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    }
}
