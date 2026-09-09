package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.dto;

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
