package top.egon.cola.platform.idp.admin.oauth.domain.vo;

/**
 * 浏览器 SSO 登录失败结果。
 *
 * <p>Browser SSO login failure result.</p>
 */
public record OAuthLoginErrorVO(String code, String message) {
}
