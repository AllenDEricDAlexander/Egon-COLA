package top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.vo;

/** Administrator-only view of a resource's suggestion and actual mapping. */
public record ResourcePermissionMappingVO(
        String resourceId,
        String applicationId,
        String resourceCode,
        String resourceType,
        String suggestedPermissionCode,
        String actualPermissionId,
        String actualPermissionCode,
        String mappingStatus,
        long resourceVersion,
        long activeRoleCount,
        boolean changed,
        long authVersion,
        long policyVersion) {

    public ResourcePermissionMappingVO {
        resourceId = required(resourceId, "resourceId");
        applicationId = required(applicationId, "applicationId");
        resourceCode = required(resourceCode, "resourceCode");
        resourceType = required(resourceType, "resourceType");
        mappingStatus = required(mappingStatus, "mappingStatus");
        if (resourceVersion < 0L || activeRoleCount < 0L
                || authVersion < 0L || policyVersion < 0L) {
            throw new IllegalArgumentException("mapping versions/counts must not be negative");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
