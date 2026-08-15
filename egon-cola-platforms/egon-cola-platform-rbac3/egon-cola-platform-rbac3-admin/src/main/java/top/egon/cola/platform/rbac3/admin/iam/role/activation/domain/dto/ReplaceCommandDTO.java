package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.dto;

import java.util.List;
import java.util.Objects;

/**
 * Command to replace a user's active root roles.
 */
public record ReplaceCommandDTO(
        String tenantId,
        String identitySub,
        String userId,
        List<String> requestedRoleIds,
        long expectedAuthVersion,
        String actorId,
        String commandId) {

    public ReplaceCommandDTO {
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        userId = required(userId, "userId");
        requestedRoleIds = List.copyOf(Objects.requireNonNull(requestedRoleIds, "requestedRoleIds"));
        actorId = required(actorId, "actorId");
        commandId = required(commandId, "commandId");
        if (expectedAuthVersion < 0) {
            throw new IllegalArgumentException("expectedAuthVersion must not be negative");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
