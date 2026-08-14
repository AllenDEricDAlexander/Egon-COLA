package top.egon.cola.platform.idp.admin.identity.domain.vo;

/**
 * 密码重置结果及新的安全版本信息。
 *
 * <p>Password-reset result together with the new security-version information.</p>
 */
public record ResetPasswordVO(
        String subject,
        String oneTimePassword,
        boolean mustChangePassword
) {
}
