package top.egon.cola.platform.rbac3.admin.application.port;

import java.time.Instant;
import java.util.Map;

public interface AuditPort {

    void append(AuditEvent event);

    record AuditEvent(
            String tenantId,
            String eventType,
            String actorId,
            String targetType,
            String targetId,
            String requestId,
            String traceId,
            Map<String, String> safeEvidence,
            Instant occurredAt
    ) {
        public AuditEvent {
            safeEvidence = Map.copyOf(safeEvidence);
        }
    }
}
