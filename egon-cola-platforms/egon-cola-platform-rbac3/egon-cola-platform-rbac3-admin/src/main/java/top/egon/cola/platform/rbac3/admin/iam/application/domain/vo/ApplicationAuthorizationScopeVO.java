package top.egon.cola.platform.rbac3.admin.iam.application.domain.vo;

/** Tenant-local RBAC authorization scope backed by a DDC Application. */
public record ApplicationAuthorizationScopeVO(
        String applicationId,
        String ddcBusinessId,
        String ddcApplicationId,
        String businessCode,
        String applicationCode,
        String applicationName,
        String status,
        int displayPriority,
        long version) {
}
