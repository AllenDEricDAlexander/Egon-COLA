package top.egon.cola.platform.idp.admin.oauth.domain.dto;

/**
 * 浏览器 SSO 登录输入。
 *
 * <p>Browser SSO login input.</p>
 */
public record OAuthLoginDTO(String tenantId, String username, String password) {
}
