package top.egon.cola.platform.rbac3.admin.iam.permission.domain.vo;

/** Global permission catalog view. */
public record PermissionCatalogVO(
        String id,
        String applicationId,
        String permissionCode,
        String permissionName,
        String riskLevel,
        String status,
        String sourceType,
        String sourceBuildId,
        String sourceChecksum,
        long version) {
}
