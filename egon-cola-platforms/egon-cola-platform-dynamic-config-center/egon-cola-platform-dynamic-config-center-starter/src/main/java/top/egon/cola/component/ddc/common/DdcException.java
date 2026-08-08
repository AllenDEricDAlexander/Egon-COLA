package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * 表示 DDC 领域或远程交互失败的统一异常。 Represents a common exception for DDC domain and remote-interaction failures.
 */
public class DdcException extends CommonException {

    /**
     * 序列化版本标识。 Serialization version identifier.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用标准错误状态创建异常。 Creates an exception from a standard error status.
     *
     * @param errorStatus 错误状态。 error status
     */
    public DdcException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    /**
     * 使用标准错误状态和根因创建异常。 Creates an exception from a standard error status and cause.
     *
     * @param errorStatus 错误状态。 error status
     * @param cause       根因。 root cause
     */
    public DdcException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    /**
     * 使用远端返回的完整错误三元组创建异常。 Creates an exception from the complete error tuple returned remotely.
     *
     * @param code    数值错误码。 numeric error code
     * @param status  状态标识。 status identifier
     * @param message 错误消息。 error message
     */
    public DdcException(int code, String status, String message) {
        super(code, status, message);
    }

    /**
     * 将自定义消息归类为无效请求。 Classifies a custom message as an invalid request.
     *
     * @param message 错误消息。 error message
     */
    public DdcException(String message) {
        super(
                DdcErrorStatus.INVALID_REQUEST.getCode(),
                DdcErrorStatus.INVALID_REQUEST.getStatus(),
                message
        );
    }

    /**
     * 将带根因的自定义消息归类为内部故障。 Classifies a custom message with a cause as an internal failure.
     *
     * @param message 错误消息。 error message
     * @param cause   根因。 root cause
     */
    public DdcException(String message, Throwable cause) {
        super(
                DdcErrorStatus.INTERNAL_FAILURE.getCode(),
                DdcErrorStatus.INTERNAL_FAILURE.getStatus(),
                message,
                cause
        );
    }
}
