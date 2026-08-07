package top.egon.cola.component.gateway.contract.rule;

/**
 * HTTP/WebSocket 路由的传输级参数。
 *
 * <p>字段使用可空包装类型，以便区分“未覆盖 profile 默认值”和“显式设置为零/false”。
 */
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
