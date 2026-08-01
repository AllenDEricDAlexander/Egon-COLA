package top.egon.cola.platform.rbac3.admin.session.application;

import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Records one append-only audit entry and one reliable runtime event for a terminal session.
 */
public final class SessionSecurityEventRecorder {

    private final AuditPort auditPort;
    private final AuthorizationEventPort eventPort;

    public SessionSecurityEventRecorder(
            AuditPort auditPort,
            AuthorizationEventPort eventPort) {
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.eventPort = Objects.requireNonNull(eventPort, "eventPort");
    }

    public void record(Termination termination) {
        Objects.requireNonNull(termination, "termination");
        String correlationId = "session:" + termination.sessionId()
                + ':' + termination.sessionVersion();
        Map<String, String> evidence = Map.of(
                "userId", termination.userId(),
                "status", termination.status(),
                "reason", termination.reason(),
                "sessionVersion", Long.toString(termination.sessionVersion()));
        boolean replayDetected = "REFRESH_TOKEN_REUSED".equals(termination.reason());
        auditPort.append(new AuditPort.AuditEvent(
                termination.tenantId(), auditEventType(termination), termination.actorId(),
                "SESSION", termination.sessionId(), correlationId, correlationId,
                evidence, termination.occurredAt(),
                replayDetected ? "DENIED" : "SUCCESS",
                replayDetected ? "CRITICAL" : "INFO",
                termination.reason()));
        eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                termination.tenantId(), "SESSION", termination.sessionId(),
                "SESSION_REVOKED", evidence, correlationId));
    }

    private String auditEventType(Termination termination) {
        if ("REFRESH_TOKEN_REUSED".equals(termination.reason())) {
            return "REFRESH_TOKEN_REPLAY_DETECTED";
        }
        if ("LOGGED_OUT".equals(termination.status())) {
            return "SESSION_LOGGED_OUT";
        }
        return "SESSION_REVOKED";
    }

    public record Termination(
            String tenantId,
            String userId,
            String sessionId,
            long sessionVersion,
            String status,
            String reason,
            String actorId,
            Instant occurredAt
    ) {

        public Termination {
            tenantId = required(tenantId, "tenantId");
            userId = required(userId, "userId");
            sessionId = required(sessionId, "sessionId");
            status = required(status, "status");
            reason = required(reason, "reason");
            actorId = required(actorId, "actorId");
            Objects.requireNonNull(occurredAt, "occurredAt");
            if (sessionVersion < 1) {
                throw new IllegalArgumentException("sessionVersion must be positive");
            }
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
