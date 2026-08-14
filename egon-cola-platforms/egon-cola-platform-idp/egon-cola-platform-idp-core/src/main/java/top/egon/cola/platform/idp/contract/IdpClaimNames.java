package top.egon.cola.platform.idp.contract;

/**
 * IdP JWT 使用的稳定自定义 Claim 名称。
 *
 * <p>Stable custom claim names used by IdP JWTs.</p>
 */
public final class IdpClaimNames {

    /** 租户标识。 / Tenant identifier. */
    public static final String TENANT_ID = "tid";
    /** OAuth Client 标识。 / OAuth Client identifier. */
    public static final String CLIENT_ID = "client_id";
    /** USER 或 SERVICE 主体类型。 / USER or SERVICE principal type. */
    public static final String PRINCIPAL_TYPE = "principal_type";
    /** Resource Server 版本。 / Resource Server version. */
    public static final String RESOURCE_VERSION = "resource_version";
    /** OAuth Scope。 / OAuth scopes. */
    public static final String SCOPE = "scope";
    /** 源业务域。 / Source business domain. */
    public static final String SOURCE_BIZ = "source_biz";
    /** 源应用。 / Source application. */
    public static final String SOURCE_APP = "source_app";
    /** 源环境。 / Source environment. */
    public static final String SOURCE_ENV = "source_env";
    /** Client 公钥 kid。 / Client public-key kid. */
    public static final String CREDENTIAL_ID = "credential_id";
    /** JWT 专用用途。 / Dedicated JWT use. */
    public static final String TOKEN_USE = "token_use";
    /** Resource 业务域。 / Resource business domain. */
    public static final String BIZ_CODE = "biz";
    /** Resource 应用。 / Resource application. */
    public static final String APP_CODE = "app";
    /** Resource 环境。 / Resource environment. */
    public static final String ENVIRONMENT = "env";
    /** Resource 实例。 / Resource instance. */
    public static final String INSTANCE_ID = "instance_id";

    /**
     * 禁止实例化常量类。
     *
     * <p>Prevents instantiation of the constants holder.</p>
     */
    private IdpClaimNames() {
    }
}
