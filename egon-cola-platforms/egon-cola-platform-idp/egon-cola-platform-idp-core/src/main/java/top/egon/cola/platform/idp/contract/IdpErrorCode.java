package top.egon.cola.platform.idp.contract;

/**
 * IdP 内部和适配层共用的稳定错误码。
 *
 * <p>Stable error codes shared by IdP core and adapters.</p>
 */
public enum IdpErrorCode {
    /** 用户凭据无效。 / User credentials are invalid. */
    INVALID_CREDENTIALS,
    /** 用户被禁用。 / User is disabled. */
    USER_DISABLED,
    /** 用户被锁定。 / User is locked. */
    USER_LOCKED,
    /** OAuth Client 无效。 / OAuth Client is invalid. */
    INVALID_CLIENT,
    /** Redirect URI 无效。 / Redirect URI is invalid. */
    INVALID_REDIRECT_URI,
    /** 租户无效。 / Tenant is invalid. */
    INVALID_TENANT,
    /** Authorization Code 无效。 / Authorization Code is invalid. */
    INVALID_AUTHORIZATION_CODE,
    /** OAuth Grant 无效。 / OAuth Grant is invalid. */
    INVALID_GRANT,
    /** Access Token 无效。 / Access Token is invalid. */
    INVALID_TOKEN,
    /** Token 或 Assertion 被重放。 / Token or Assertion was replayed. */
    TOKEN_REPLAYED,
    /** 身份运行态不可用。 / Identity runtime state is unavailable. */
    IDENTITY_STATE_UNAVAILABLE,
    /** Resource Server 不存在。 / Resource Server does not exist. */
    IDP_RESOURCE_SERVER_NOT_FOUND,
    /** Resource Server 已禁用。 / Resource Server is disabled. */
    IDP_RESOURCE_SERVER_DISABLED,
    /** Resource Server 业务域不匹配。 / Resource Server business domain mismatches. */
    IDP_RESOURCE_SERVER_BIZ_MISMATCH,
    /** Resource Server 应用不匹配。 / Resource Server application mismatches. */
    IDP_RESOURCE_SERVER_APP_MISMATCH,
    /** Resource Server 环境不匹配。 / Resource Server environment mismatches. */
    IDP_RESOURCE_SERVER_ENV_MISMATCH,
    /** Resource Server 凭证无效。 / Resource Server credential is invalid. */
    IDP_RESOURCE_SERVER_CREDENTIAL_INVALID,
    /** Client Assertion Audience 无效。 / Client Assertion audience is invalid. */
    IDP_CLIENT_ASSERTION_AUDIENCE_INVALID,
    /** Client Assertion 已被使用。 / Client Assertion was already used. */
    IDP_CLIENT_ASSERTION_REPLAYED,
    /** Resource Server 准入暂时不可用。 / Resource Server admission is unavailable. */
    IDP_RESOURCE_ADMISSION_UNAVAILABLE,
    /** USER Resource Grant 不存在。 / USER Resource Grant does not exist. */
    IDP_USER_RESOURCE_GRANT_NOT_FOUND,
    /** Service Resource Grant 不存在。 / Service Resource Grant does not exist. */
    IDP_SERVICE_RESOURCE_GRANT_NOT_FOUND,
    /** SERVICE Scope 无效。 / SERVICE scope is invalid. */
    IDP_SERVICE_SCOPE_INVALID,
    /** USER 无目标应用入口权限。 / USER lacks target application entry permission. */
    IDP_RESOURCE_ACCESS_DENIED
}
