package top.egon.cola.component.gateway.contract.rule;

/**
 * 路由响应向调用方呈现的传输模式。
 */
public enum GatewayTransportResponseMode {
    STANDARD,
    AUTO_STREAM,
    SSE,
    BINARY_STREAM
}
