package top.egon.cola.platform.idp.admin.identity.domain.vo;

import java.time.Instant;

/**
 * 管理端展示的统一身份用户信息。
 *
 * <p>Identity-user information returned by administration APIs.</p>
 */
public record IdentityUserVO(
        String subject,
        String username,
        String displayName,
        String status,
        long tokenVersion,
        int failedLoginCount,
        Instant lockedUntil,
        Instant lastLoginAt,
        long version
) {
}
