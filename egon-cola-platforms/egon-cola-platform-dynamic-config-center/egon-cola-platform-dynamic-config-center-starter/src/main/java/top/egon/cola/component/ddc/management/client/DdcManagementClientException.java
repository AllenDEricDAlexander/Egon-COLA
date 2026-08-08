package top.egon.cola.component.ddc.management.client;

/**
 * DDC 管理客户端在服务端拒绝请求、本地序列化或传输失败时抛出的异常。 /
 * Exception raised by the DDC management client for server rejection, local serialization, or transport failure.
 */
public final class DdcManagementClientException extends RuntimeException {

    /** 服务端业务错误码；本地失败时为 {@code -1}。 / Server business error code, or {@code -1} for local failures. */
    private final int code;

    /** 稳定错误状态标识。 / Stable error-status identifier. */
    private final String status;

    /**
     * 使用服务端业务错误信息构造异常。 / Constructs an exception from server business-error information.
     *
     * @param code 服务端业务错误码 / server business error code
     * @param status 稳定错误状态标识 / stable error-status identifier
     * @param message 可读错误消息 / human-readable error message
     */
    public DdcManagementClientException(int code, String status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * 使用本地失败原因构造异常，并将业务错误码设为 {@code -1}。 /
     * Constructs an exception for a local failure and assigns business error code {@code -1}.
     *
     * @param status 稳定错误状态标识 / stable error-status identifier
     * @param message 可读错误消息 / human-readable error message
     * @param cause 原始失败原因 / original failure cause
     */
    public DdcManagementClientException(String status, String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.status = status;
    }

    /**
     * 返回服务端业务错误码。 / Returns the server business error code.
     *
     * @return 服务端错误码；本地失败时为 {@code -1} / server error code, or {@code -1} for local failures
     */
    public int code() {
        return code;
    }

    /**
     * 返回稳定错误状态标识。 / Returns the stable error-status identifier.
     *
     * @return 错误状态标识 / error-status identifier
     */
    public String status() {
        return status;
    }
}
