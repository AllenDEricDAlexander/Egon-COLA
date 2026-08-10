package top.egon.cola.platform.idp.admin.oauth.domain.vo;

/**
 * 浏览器 SSO 登录成功结果。
 *
 * <p>Successful browser SSO login result.</p>
 */
public record OAuthLoginVO(
        String identitySub,
        String displayName,
        boolean mustChangePassword
) {
}
