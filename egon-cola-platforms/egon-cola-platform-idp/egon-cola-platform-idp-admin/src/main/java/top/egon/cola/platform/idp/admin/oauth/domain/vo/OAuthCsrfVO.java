package top.egon.cola.platform.idp.admin.oauth.domain.vo;

/**
 * 浏览器 SSO 登录使用的 CSRF 令牌视图。
 *
 * <p>CSRF token view used by browser SSO login.</p>
 */
public record OAuthCsrfVO(String token) {
}
