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
            Instant occurredAt,
            String outcome,
            String severity,
            String reasonCode
    ) {
        public AuditEvent(
                String tenantId,
                String eventType,
                String actorId,
                String targetType,
                String targetId,
                String requestId,
                String traceId,
                Map<String, String> safeEvidence,
                Instant occurredAt) {
            this(tenantId, eventType, actorId, targetType, targetId,
                    requestId, traceId, safeEvidence, occurredAt,
                    "SUCCESS", "INFO", "ALLOW");
        }

        public AuditEvent {
            safeEvidence = Map.copyOf(safeEvidence);
        }
    }
}
