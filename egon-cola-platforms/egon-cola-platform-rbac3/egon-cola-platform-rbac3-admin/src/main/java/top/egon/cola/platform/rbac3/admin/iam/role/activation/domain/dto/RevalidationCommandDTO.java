package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.dto;

import java.time.Instant;

/**
 * User authorization revalidation command.
 */
public record RevalidationCommandDTO(
        String tenantId,
        String userId,
        Instant databaseNow,
        String actorId) {
}
