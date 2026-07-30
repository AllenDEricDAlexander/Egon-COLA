package top.egon.cola.component.gateway.engine.http.proxy;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;

import java.util.Objects;

public final class GatewayHttpProxyStrategySelector {

    private final GatewayHttpProxyStrategy aggregated;

    private final GatewayHttpProxyStrategy streaming;

    public GatewayHttpProxyStrategySelector(
            GatewayHttpProxyStrategy aggregated,
            GatewayHttpProxyStrategy streaming) {
        this.aggregated = Objects.requireNonNull(aggregated, "aggregated");
        this.streaming = Objects.requireNonNull(streaming, "streaming");
    }

    public GatewayHttpProxyStrategy select(GatewayRequestBodyMode mode) {
        return switch (Objects.requireNonNull(mode, "mode")) {
            case AGGREGATED -> aggregated;
            case STREAMING -> streaming;
        };
    }
}
