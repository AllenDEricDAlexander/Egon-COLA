package top.egon.cola.component.gateway.starter.annotation;

/**
 * HTTP 请求参数在协议中的位置。
 *
 * <p>RPC 只有一个完整 Protobuf 请求消息，不使用位置声明。
 */
public enum GatewayRequestLocation {
    PATH,
    QUERY,
    HEADER,
    COOKIE,
    BODY,
    PART
}
