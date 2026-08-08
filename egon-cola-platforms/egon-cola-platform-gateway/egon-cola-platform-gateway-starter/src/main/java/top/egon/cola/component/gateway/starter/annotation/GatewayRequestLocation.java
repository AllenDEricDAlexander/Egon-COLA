package top.egon.cola.component.gateway.starter.annotation;

/**
 * Location of an HTTP request value within the protocol request.
 *
 * <p>RPC operations receive one complete Protobuf request message and therefore
 * do not use request-location declarations.
 */
public enum GatewayRequestLocation {
    /** A value captured from a URI path variable. */
    PATH,

    /** A value supplied in the URI query string. */
    QUERY,

    /** A value supplied in an HTTP request header. */
    HEADER,

    /** A value supplied in an HTTP cookie. */
    COOKIE,

    /** The HTTP request body. */
    BODY,

    /** A part of a multipart HTTP request. */
    PART
}
