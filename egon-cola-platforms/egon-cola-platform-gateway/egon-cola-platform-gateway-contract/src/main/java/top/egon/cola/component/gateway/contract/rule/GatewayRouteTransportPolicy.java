package top.egon.cola.component.gateway.contract.rule;

public record GatewayRouteTransportPolicy(
        GatewayRouteProfile profile,
        GatewayTransportProtocol transportProtocol,
        GatewayRequestBodyMode requestBodyMode,
        GatewayTransportResponseMode responseMode,
        Long maxRequestBodyBytes,
        Long connectTimeoutMs,
        Long responseHeaderTimeoutMs,
        Long streamIdleTimeoutMs,
        Long totalTimeoutMs,
        Long websocketIdleTimeoutMs,
        Long websocketMaxFrameBytes,
        Boolean bodyLogEnabled,
        Boolean retryEnabled
) {
}
