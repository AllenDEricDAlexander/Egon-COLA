package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** Trusted server-enriched command; it deliberately has no permission field. */
public record ReplaceRoleResourcesCommandDTO(
        Long tenantId,
        Long applicationId,
        Long roleId,
        Set<Long> resourceIds,
        Instant validFrom,
        Instant validTo,
        long expectedRoleVersion,
        String actorId) {

    public ReplaceRoleResourcesCommandDTO {
        tenantId = positive(tenantId, "tenantId");
        applicationId = positive(applicationId, "applicationId");
        roleId = positive(roleId, "roleId");
        resourceIds = Set.copyOf(Objects.requireNonNull(resourceIds, "resourceIds"));
        resourceIds.forEach(id -> positive(id, "resourceId"));
        validFrom = Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        if (expectedRoleVersion < 0L) {
            throw new IllegalArgumentException("expectedRoleVersion must not be negative");
        }
        actorId = required(actorId, "actorId");
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
