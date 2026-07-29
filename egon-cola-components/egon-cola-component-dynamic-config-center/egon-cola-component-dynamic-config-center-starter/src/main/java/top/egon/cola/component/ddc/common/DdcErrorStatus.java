package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.core.enums.ErrorStatus;

public enum DdcErrorStatus implements ErrorStatus {

    INVALID_REQUEST(56000, "DDC_INVALID_REQUEST", "invalid DDC request"),
    LEASE_NOT_FOUND(56001, "DDC_LEASE_NOT_FOUND", "lease not found"),
    LEASE_MISMATCH(56002, "DDC_LEASE_MISMATCH", "lease mismatch"),
    INSTANCE_ID_CONFLICT(56003, "DDC_INSTANCE_ID_CONFLICT", "instance id conflict"),
    PUBLISH_IN_PROGRESS(56010, "DDC_PUBLISH_IN_PROGRESS", "publish already in progress"),
    NO_LIVE_INSTANCE(56011, "DDC_NO_LIVE_INSTANCE", "no live config instance"),
    CHANGE_ID_CONFLICT(56012, "DDC_CHANGE_ID_CONFLICT", "change id conflict"),
    TARGET_LEASE_EXPIRED(56013, "DDC_TARGET_LEASE_EXPIRED", "publish target lease expired"),
    SIGNATURE_REQUIRED(56020, "DDC_SIGNATURE_REQUIRED", "signature required"),
    SIGNATURE_INVALID(56021, "DDC_SIGNATURE_INVALID", "signature invalid"),
    SIGNATURE_EXPIRED(56022, "DDC_SIGNATURE_EXPIRED", "signature expired"),
    SIGNATURE_REPLAY(56023, "DDC_SIGNATURE_REPLAY", "signature nonce replayed"),
    INTERNAL_FAILURE(56999, "DDC_INTERNAL_FAILURE", "DDC internal failure");

    private final int code;

    private final String status;

    private final String message;

    DdcErrorStatus(int code, String status, String message) {
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
