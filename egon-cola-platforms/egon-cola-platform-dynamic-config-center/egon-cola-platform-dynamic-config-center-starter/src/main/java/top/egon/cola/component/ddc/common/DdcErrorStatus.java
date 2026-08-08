package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/**
 * 汇总 DDC 对外返回的稳定错误码、状态标识和默认消息。 Enumerates stable DDC error codes, status identifiers, and default messages exposed to callers.
 */
public enum DdcErrorStatus implements ErrorStatus {

    /**
     * 请求字段、格式或业务约束无效。 The request violates field, format, or business constraints.
     */
    INVALID_REQUEST(56000, "DDC_INVALID_REQUEST", "invalid DDC request"),
    /**
     * 指定租约不存在或已不可用。 The requested lease does not exist or is no longer available.
     */
    LEASE_NOT_FOUND(56001, "DDC_LEASE_NOT_FOUND", "lease not found"),
    /**
     * 请求中的租约标识与实例当前租约不一致。 The supplied lease identifier does not match the instance's current lease.
     */
    LEASE_MISMATCH(56002, "DDC_LEASE_MISMATCH", "lease mismatch"),
    /**
     * 实例标识已被不兼容的注册占用。 The instance identifier is already held by an incompatible registration.
     */
    INSTANCE_ID_CONFLICT(56003, "DDC_INSTANCE_ID_CONFLICT", "instance id conflict"),
    /**
     * 同一作用域已有发布流程正在执行。 A publication is already running for the same scope.
     */
    PUBLISH_IN_PROGRESS(56010, "DDC_PUBLISH_IN_PROGRESS", "publish already in progress"),
    /**
     * 发布作用域内没有可接收配置的存活实例。 No live configuration instance can receive the publication.
     */
    NO_LIVE_INSTANCE(56011, "DDC_NO_LIVE_INSTANCE", "no live config instance"),
    /**
     * 相同变更标识对应了不同的发布内容。 The same change identifier was reused for different publication content.
     */
    CHANGE_ID_CONFLICT(56012, "DDC_CHANGE_ID_CONFLICT", "change id conflict"),
    /**
     * 发布目标的租约在确认前已经过期。 A target lease expired before publication acknowledgement.
     */
    TARGET_LEASE_EXPIRED(56013, "DDC_TARGET_LEASE_EXPIRED", "publish target lease expired"),
    /**
     * 当前接口要求签名但请求未提供签名信息。 The endpoint requires signing but the request omitted signature data.
     */
    SIGNATURE_REQUIRED(56020, "DDC_SIGNATURE_REQUIRED", "signature required"),
    /**
     * 请求签名与服务端计算结果不一致。 The request signature does not match the server calculation.
     */
    SIGNATURE_INVALID(56021, "DDC_SIGNATURE_INVALID", "signature invalid"),
    /**
     * 请求时间戳超出允许的签名有效窗口。 The request timestamp falls outside the allowed signature window.
     */
    SIGNATURE_EXPIRED(56022, "DDC_SIGNATURE_EXPIRED", "signature expired"),
    /**
     * 签名 nonce 已被使用，疑似重放请求。 The signature nonce has already been used, indicating a replay.
     */
    SIGNATURE_REPLAY(56023, "DDC_SIGNATURE_REPLAY", "signature nonce replayed"),
    /**
     * 指定业务不存在。 The requested business does not exist.
     */
    BIZ_NOT_FOUND(56030, "DDC_BIZ_NOT_FOUND", "biz not found"),
    /**
     * 待创建的业务编码已经存在。 The business code being created already exists.
     */
    BIZ_CODE_EXISTS(56031, "DDC_BIZ_CODE_EXISTS", "biz code already exists"),
    /**
     * 业务仍包含应用，不能执行删除。 The business still owns applications and cannot be deleted.
     */
    BIZ_IN_USE(56032, "DDC_BIZ_IN_USE", "biz still has apps"),
    /**
     * 指定应用不存在。 The requested application does not exist.
     */
    APP_NOT_FOUND(56033, "DDC_APP_NOT_FOUND", "app not found"),
    /**
     * 业务下待创建的应用编码已经存在。 The application code being created already exists in the business.
     */
    APP_CODE_EXISTS(56034, "DDC_APP_CODE_EXISTS", "app code already exists"),
    /**
     * 应用仍包含命名空间，不能执行删除。 The application still owns namespaces and cannot be deleted.
     */
    APP_IN_USE(56035, "DDC_APP_IN_USE", "app still has namespaces"),
    /**
     * 指定命名空间不存在。 The requested namespace does not exist.
     */
    NAMESPACE_NOT_FOUND(56036, "DDC_NAMESPACE_NOT_FOUND", "namespace not found"),
    /**
     * 待创建的命名空间编码已经存在。 The namespace code being created already exists.
     */
    NAMESPACE_CODE_EXISTS(56037, "DDC_NAMESPACE_CODE_EXISTS", "namespace already exists"),
    /**
     * 命名空间仍包含配置，不能执行删除。 The namespace still contains configurations and cannot be deleted.
     */
    NAMESPACE_IN_USE(56038, "DDC_NAMESPACE_IN_USE", "namespace still has configs"),
    /**
     * 指定环境不存在。 The requested environment does not exist.
     */
    ENV_NOT_FOUND(56039, "DDC_ENV_NOT_FOUND", "env not found"),
    /**
     * 待创建的环境编码已经存在。 The environment code being created already exists.
     */
    ENV_CODE_EXISTS(56040, "DDC_ENV_CODE_EXISTS", "env code already exists"),
    /**
     * 环境仍被作用域绑定引用，不能执行删除。 The environment is still referenced by scope bindings and cannot be deleted.
     */
    ENV_IN_USE(56041, "DDC_ENV_IN_USE", "env is still referenced"),
    /**
     * 请求命中的业务-环境-应用作用域已禁用。 The resolved business-environment-application scope is disabled.
     */
    SCOPE_DISABLED(56042, "DDC_SCOPE_DISABLED", "scope disabled"),
    /**
     * 相同的命名空间、环境和应用绑定已经存在。 The same namespace, environment, and application binding already exists.
     */
    NAMESPACE_BINDING_EXISTS(
            56043,
            "DDC_NAMESPACE_BINDING_EXISTS",
            "namespace environment app binding already exists"
    ),
    /**
     * 指定的命名空间、环境和应用绑定不存在。 The requested namespace, environment, and application binding does not exist.
     */
    NAMESPACE_BINDING_NOT_FOUND(
            56044,
            "DDC_NAMESPACE_BINDING_NOT_FOUND",
            "namespace environment app binding not found"
    ),
    /**
     * DDC 内部处理发生未归类故障。 An unclassified internal DDC processing failure occurred.
     */
    INTERNAL_FAILURE(56999, "DDC_INTERNAL_FAILURE", "DDC internal failure");

    /**
     * 对外稳定数值错误码。 Stable numeric error code exposed to callers.
     */
    private final int code;

    /**
     * 对外稳定状态标识。 Stable status identifier exposed to callers.
     */
    private final String status;

    /**
     * 默认英文错误消息。 Default English error message.
     */
    private final String message;

    /**
     * 创建错误状态常量。 Creates an error-status constant.
     *
     * @param code    数值错误码。 numeric error code
     * @param status  状态标识。 status identifier
     * @param message 默认消息。 default message
     */
    DdcErrorStatus(int code, String status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    /**
     * 返回对外数值错误码。 Returns the externally visible numeric error code.
     *
     * @return 数值错误码。 numeric error code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * 返回对外状态标识。 Returns the externally visible status identifier.
     *
     * @return 状态标识。 status identifier
     */
    @Override
    public String getStatus() {
        return status;
    }

    /**
     * 返回默认错误消息。 Returns the default error message.
     *
     * @return 默认错误消息。 default error message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
