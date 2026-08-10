package top.egon.cola.platform.idp.admin.identity.domain.vo;

/**
 * 新建身份用户及其一次性初始密码。
 *
 * <p>Newly created identity user together with its one-time initial password.</p>
 */
public record CreatedIdentityUserVO(
        String subject,
        String username,
        String displayName,
        String status,
        String oneTimePassword
) {
}
