package top.egon.cola.platform.rbac3.admin.application;

import java.time.Instant;

public record CommandContext(
        String tenantId,
        String operatorUserId,
        String sessionId,
        String requestId,
        String traceId,
        Instant databaseNow
) {
}
