package top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.vo;

/**
 * RBAC directory view for the local authorization projection. Identity
 * profile data is owned by Tianquan-Shoubing and is intentionally absent from this view.
 */
public record UserDirectoryVO(
        String userId,
        String identitySub,
        String status,
        long authVersion
) {
}
