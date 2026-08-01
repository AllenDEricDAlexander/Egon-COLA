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
    BIZ_NOT_FOUND(56030, "DDC_BIZ_NOT_FOUND", "biz not found"),
    BIZ_CODE_EXISTS(56031, "DDC_BIZ_CODE_EXISTS", "biz code already exists"),
    BIZ_IN_USE(56032, "DDC_BIZ_IN_USE", "biz still has apps"),
    APP_NOT_FOUND(56033, "DDC_APP_NOT_FOUND", "app not found"),
    APP_CODE_EXISTS(56034, "DDC_APP_CODE_EXISTS", "app code already exists"),
    APP_IN_USE(56035, "DDC_APP_IN_USE", "app still has namespaces"),
    NAMESPACE_NOT_FOUND(56036, "DDC_NAMESPACE_NOT_FOUND", "namespace not found"),
    NAMESPACE_CODE_EXISTS(56037, "DDC_NAMESPACE_CODE_EXISTS", "namespace already exists"),
    NAMESPACE_IN_USE(56038, "DDC_NAMESPACE_IN_USE", "namespace still has configs"),
    ENV_NOT_FOUND(56039, "DDC_ENV_NOT_FOUND", "env not found"),
    ENV_CODE_EXISTS(56040, "DDC_ENV_CODE_EXISTS", "env code already exists"),
    ENV_IN_USE(56041, "DDC_ENV_IN_USE", "env is still referenced"),
    SCOPE_DISABLED(56042, "DDC_SCOPE_DISABLED", "scope disabled"),
    NAMESPACE_BINDING_EXISTS(
            56043,
            "DDC_NAMESPACE_BINDING_EXISTS",
            "namespace environment app binding already exists"
    ),
    NAMESPACE_BINDING_NOT_FOUND(
            56044,
            "DDC_NAMESPACE_BINDING_NOT_FOUND",
            "namespace environment app binding not found"
    ),
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
