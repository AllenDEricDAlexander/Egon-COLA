package top.egon.cola.component.ddc.management.client;

import top.egon.cola.component.common.core.code.ErrorStatus;

public enum DdcManagementErrorCode implements ErrorStatus {

    CONFIG_NOT_FOUND(56004, "DDC_CONFIG_NOT_FOUND", "config not found"),
    PUBLISH_TASK_NOT_FOUND(56014, "DDC_PUBLISH_TASK_NOT_FOUND", "publish task not found");

    private final int code;

    private final String status;

    private final String message;

    DdcManagementErrorCode(int code, String status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
