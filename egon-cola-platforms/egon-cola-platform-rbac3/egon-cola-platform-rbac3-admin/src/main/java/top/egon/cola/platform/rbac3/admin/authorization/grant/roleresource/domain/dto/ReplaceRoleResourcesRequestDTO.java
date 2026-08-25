package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Untrusted HTTP body for replacing a role's complete direct resource set. */
public record ReplaceRoleResourcesRequestDTO(
        List<String> resourceIds,
        Instant validFrom,
        Instant validTo,
        long expectedRoleVersion) {

    public ReplaceRoleResourcesRequestDTO {
        resourceIds = List.copyOf(Objects.requireNonNull(resourceIds, "resourceIds"));
        if (resourceIds.size() > 2000) {
            throw new IllegalArgumentException("resourceIds must contain at most 2000 items");
        }
        if (new HashSet<>(resourceIds).size() != resourceIds.size()) {
            throw new IllegalArgumentException("resourceIds must be unique");
        }
        resourceIds = resourceIds.stream().map(value -> positiveDecimal(value, "resourceId"))
                .toList();
        if (validTo != null && validFrom != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        if (expectedRoleVersion < 0L) {
            throw new IllegalArgumentException("expectedRoleVersion must not be negative");
        }
    }

    private static String positiveDecimal(String value, String fieldName) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(fieldName + " must be a positive decimal id");
        }
        return value;
    }
}
