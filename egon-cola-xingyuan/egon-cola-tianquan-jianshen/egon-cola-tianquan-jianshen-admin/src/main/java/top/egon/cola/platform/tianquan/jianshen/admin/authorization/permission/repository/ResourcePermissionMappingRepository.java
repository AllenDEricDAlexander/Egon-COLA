package top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.repository;

import java.time.Instant;

/** Persistence boundary for the one authoritative Resource.requiredPermissionId mapping. */
public interface ResourcePermissionMappingRepository {

    MappingFacts loadForUpdate(Long resourceId);

    PermissionFacts activePermission(Long permissionId);

    long countActiveRoleUsage(MappingFacts resource, Instant now);

    MappingFacts updateActualMapping(
            MappingFacts resource,
            Long permissionId,
            String actorId,
            String reason,
            Instant now);

    record MappingFacts(
            Long resourceId,
            Long applicationId,
            String resourceCode,
            String resourceType,
            String status,
            Long requiredPermissionId,
            String suggestedPermissionCode,
            long version) {
        public MappingFacts {
            resourceId = positive(resourceId, "resourceId");
            applicationId = positive(applicationId, "applicationId");
            resourceCode = required(resourceCode, "resourceCode");
            resourceType = required(resourceType, "resourceType");
            status = required(status, "status");
            if (version < 0L) {
                throw new IllegalArgumentException("version must not be negative");
            }
        }
    }

    record PermissionFacts(
            Long permissionId,
            Long applicationId,
            String permissionCode,
            String status) {
        public PermissionFacts {
            permissionId = positive(permissionId, "permissionId");
            applicationId = positive(applicationId, "applicationId");
            permissionCode = required(permissionCode, "permissionCode");
            status = required(status, "status");
        }
    }

    private static Long positive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
