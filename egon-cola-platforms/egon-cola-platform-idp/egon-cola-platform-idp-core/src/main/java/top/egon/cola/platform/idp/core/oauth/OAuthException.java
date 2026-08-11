package top.egon.cola.platform.idp.core.oauth;

/**
 * OAuth 协议错误以及仅供服务端审计的稳定内部错误码。
 *
 * <p>OAuth protocol error carrying a stable internal code used only for server-side auditing.</p>
 */
public final class OAuthException extends RuntimeException {

    /** 对外 OAuth 错误码；public OAuth error code. */
    private final String oauthError;

    /** 不进入协议响应的内部稳定码；internal stable code excluded from protocol responses. */
    private final String internalCode;

    /**
     * 创建内部码与 OAuth 错误码相同的兼容异常。
     *
     * <p>Creates a backward-compatible exception whose internal code equals its OAuth error.</p>
     *
     * @param oauthError 对外 OAuth 错误码；public OAuth error code
     * @param message 安全错误描述；safe error description
     */
    public OAuthException(String oauthError, String message) {
        this(oauthError, message, oauthError);
    }

    /**
     * 创建协议错误与内部审计码分离的异常。
     *
     * <p>Creates an exception separating its protocol error from its internal audit code.</p>
     *
     * @param oauthError 对外 OAuth 错误码；public OAuth error code
     * @param message 安全错误描述；safe error description
     * @param internalCode 不进入响应的稳定内部码；stable internal code excluded from responses
     */
    public OAuthException(
            String oauthError,
            String message,
            String internalCode
    ) {
        super(required(message, "message"));
        this.oauthError = required(oauthError, "oauthError");
        this.internalCode = required(internalCode, "internalCode");
    }

    /**
     * 返回对外 OAuth 错误码。
     *
     * <p>Returns the public OAuth error code.</p>
     *
     * @return OAuth 错误码；OAuth error code
     */
    public String oauthError() {
        return oauthError;
    }

    /**
     * 返回仅供服务端审计和内部映射的稳定错误码。
     *
     * <p>Returns the stable code used only for server-side auditing and internal mapping.</p>
     *
     * @return 内部稳定码；internal stable code
     */
    public String internalCode() {
        return internalCode;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
