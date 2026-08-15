package top.egon.cola.platform.rbac3.admin.iam.user.domain.vo;

/**
 * RBAC directory view for the local authorization projection. Identity
 * profile data is owned by IdP and is intentionally absent from this view.
 */
public record UserDirectoryVO(
        String userId,
        String identitySub,
        String status,
        long authVersion
) {
}
