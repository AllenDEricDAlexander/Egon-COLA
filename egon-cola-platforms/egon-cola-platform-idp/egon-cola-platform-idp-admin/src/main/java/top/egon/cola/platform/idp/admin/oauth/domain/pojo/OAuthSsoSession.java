package top.egon.cola.platform.idp.admin.oauth.domain.pojo;

/**
 * 服务端保存的浏览器 SSO 会话身份引用。
 *
 * <p>Identity reference stored for a server-side browser SSO session.</p>
 */
public record OAuthSsoSession(String identitySub, String sessionId) {

    public OAuthSsoSession {
        identitySub = required(identitySub, "identitySub");
        sessionId = required(sessionId, "sessionId");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
