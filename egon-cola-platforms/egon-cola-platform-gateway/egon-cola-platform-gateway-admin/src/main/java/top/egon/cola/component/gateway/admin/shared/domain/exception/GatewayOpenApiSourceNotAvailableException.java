package top.egon.cola.component.gateway.admin.shared.domain.exception;

/**
 * Raised when an operation is not backed by an OpenAPI 3.1 source.
 *
 * <p>中文：MANUAL 与 RPC_DESCRIPTOR 操作不伪造 OpenAPI Fragment；管理端将该
 * 情况映射为稳定的 409 响应。</p>
 */
public final class GatewayOpenApiSourceNotAvailableException
        extends RuntimeException {

    /** Creates the stable source-unavailable error. */
    public GatewayOpenApiSourceNotAvailableException() {
        super("current operation has no OpenAPI source");
    }
}
