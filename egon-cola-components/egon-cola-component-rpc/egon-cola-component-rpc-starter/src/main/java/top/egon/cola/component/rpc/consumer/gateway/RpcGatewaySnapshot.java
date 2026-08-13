package top.egon.cola.component.rpc.consumer.gateway;

import java.time.Instant;
import java.util.List;

/**
 * 对当前可用 RPC Gateway 端点的一次不可变观测。
 *
 * <p>One immutable observation of the available RPC Gateway endpoints.
 */
public record RpcGatewaySnapshot(
        long revision,
        Instant observedAt,
        List<RpcGatewayEndpoint> endpoints
) {

    public RpcGatewaySnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "RPC Gateway snapshot revision must not be negative"
            );
        }
        if (observedAt == null) {
            throw new IllegalArgumentException(
                    "RPC Gateway snapshot observation time is required"
            );
        }
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    }
}
