package top.egon.cola.platform.rbac3.admin.authorization.permission.domain.dto;

import java.util.Objects;

/** Trusted only after the resource and permission are reloaded by the server. */
public record UpdateResourcePermissionMappingRequestDTO(
        String permissionId,
        long expectedResourceVersion,
        String reason) {

    public UpdateResourcePermissionMappingRequestDTO {
        permissionId = positiveDecimal(permissionId, "permissionId");
        if (expectedResourceVersion < 0L) {
            throw new IllegalArgumentException(
                    "expectedResourceVersion must not be negative");
        }
        reason = optionalReason(reason);
    }

    private static String positiveDecimal(String value, String fieldName) {
        if (value == null || !value.trim().matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(fieldName + " must be a positive decimal id");
        }
        return value.trim();
    }

    private static String optionalReason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("reason must contain at most 500 characters");
        }
        return normalized;
    }
}
