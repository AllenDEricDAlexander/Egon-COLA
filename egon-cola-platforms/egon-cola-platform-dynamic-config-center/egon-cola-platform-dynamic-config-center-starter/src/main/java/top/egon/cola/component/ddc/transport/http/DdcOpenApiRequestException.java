package top.egon.cola.component.ddc.transport.http;

/**
 * 表示 DDC OpenAPI 请求在进入网络传输前无法构造。 /
 * Indicates that a DDC OpenAPI request could not be constructed before network transport.
 */
public final class DdcOpenApiRequestException extends RuntimeException {

    /**
     * 使用固定诊断信息和底层原因创建异常。 /
     * Creates the exception with a stable diagnostic message and underlying cause.
     *
     * @param cause 请求构造失败原因 / request-construction failure
     */
    public DdcOpenApiRequestException(Throwable cause) {
        super("DDC OpenAPI request serialization failed", cause);
    }
}
