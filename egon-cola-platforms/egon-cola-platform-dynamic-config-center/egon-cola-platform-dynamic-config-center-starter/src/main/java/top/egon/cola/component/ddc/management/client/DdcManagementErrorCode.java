package top.egon.cola.component.ddc.management.client;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/**
 * DDC 管理客户端需要识别并转换为专用行为的服务端错误。 /
 * Server errors recognized by the DDC management client for specialized handling.
 */
public enum DdcManagementErrorCode implements ErrorStatus {

    /** 查询的配置不存在。 / The requested configuration does not exist. */
    CONFIG_NOT_FOUND(56004, "DDC_CONFIG_NOT_FOUND", "config not found"),
    /** 查询的发布任务不存在。 / The requested publication task does not exist. */
    PUBLISH_TASK_NOT_FOUND(56014, "DDC_PUBLISH_TASK_NOT_FOUND", "publish task not found");

    /** 数值业务错误码。 / Numeric business error code. */
    private final int code;

    /** 稳定错误状态标识。 / Stable error-status identifier. */
    private final String status;

    /** 默认可读错误消息。 / Default human-readable error message. */
    private final String message;

    /**
     * 构造客户端可识别的服务端错误。 / Constructs a server error recognized by the client.
     *
     * @param code 数值业务错误码 / numeric business error code
     * @param status 稳定错误状态标识 / stable error-status identifier
     * @param message 默认可读错误消息 / default human-readable error message
     */
    DdcManagementErrorCode(int code, String status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    /**
     * 返回数值业务错误码。 / Returns the numeric business error code.
     *
     * @return 业务错误码 / business error code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * 返回稳定错误状态标识。 / Returns the stable error-status identifier.
     *
     * @return 错误状态标识 / error-status identifier
     */
    @Override
    public String getStatus() {
        return status;
    }

    /**
     * 返回默认可读错误消息。 / Returns the default human-readable error message.
     *
     * @return 默认错误消息 / default error message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
