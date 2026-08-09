package top.egon.cola.component.ddc.error;

import java.io.Serial;

/**
 * DDC 客户端与远端之间发生的传输中立故障。
 * / Transport-neutral failure between a DDC client and its remote endpoint.
 */
public final class DdcClientTransportException extends RuntimeException {

    /** 序列化版本。 / Serialization version. */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 故障是否适合由调用方重试。 / Whether callers may retry the failure. */
    private final boolean retryable;

    /**
     * 创建不携带底层传输细节的客户端异常。
     * / Creates a client failure without exposing underlying transport detail.
     *
     * @param message 可安全展示的错误消息 / safe error message
     * @param retryable 是否可重试 / whether the failure is retryable
     */
    public DdcClientTransportException(String message, boolean retryable) {
        this(message, retryable, null);
    }

    /**
     * 创建携带根因的客户端异常。
     * / Creates a client failure with its root cause.
     *
     * @param message 可安全展示的错误消息 / safe error message
     * @param retryable 是否可重试 / whether the failure is retryable
     * @param cause 底层失败 / underlying failure
     */
    public DdcClientTransportException(
            String message,
            boolean retryable,
            Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    /**
     * 返回调用方是否可以重试。
     * / Returns whether callers may retry.
     *
     * @return 可重试时为 {@code true} / {@code true} when retryable
     */
    public boolean retryable() {
        return retryable;
    }
}
