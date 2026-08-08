package top.egon.cola.component.gateway.starter.annotation;

/**
 * Location of an HTTP request value within the protocol request.
 *
 * <p>RPC operations receive one complete Protobuf request message and therefore
 * do not use request-location declarations.
 *
 * <p>表示 HTTP 请求值在协议请求中的位置；RPC 操作接收完整 Protobuf 请求消息，
 * 不使用此类位置声明。
 */
public enum GatewayRequestLocation {
    /** A value captured from a URI path variable. 从 URI 路径变量中捕获的值。 */
    PATH,

    /** A value supplied in the URI query string. 在 URI 查询字符串中提供的值。 */
    QUERY,

    /** A value supplied in an HTTP request header. 在 HTTP 请求头中提供的值。 */
    HEADER,

    /** A value supplied in an HTTP cookie. 在 HTTP Cookie 中提供的值。 */
    COOKIE,

    /** The HTTP request body. HTTP 请求体。 */
    BODY,

    /** A part of a multipart HTTP request. multipart HTTP 请求中的一个分段。 */
    PART
}
