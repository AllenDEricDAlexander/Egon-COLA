package top.egon.cola.platform.idp.admin.audit.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_audit_log")
public class IdentityAuditLogEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "actor_sub", length = 64)
    private String actorSub;

    @Column(name = "target_sub", length = 64)
    private String targetSub;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(nullable = false, length = 32)
    private String result;

    @Column(nullable = false, length = 128)
    private String reason;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected IdentityAuditLogEntity() {
    }

    public static IdentityAuditLogEntity record(
            String id,
            String eventType,
            String actorSub,
            String targetSub,
            String result,
            String reason,
            String payload,
            Instant occurredAt
    ) {
        IdentityAuditLogEntity entity = new IdentityAuditLogEntity();
        entity.id = required(id, "id");
        entity.eventType = required(eventType, "eventType");
        entity.actorSub = optional(actorSub);
        entity.targetSub = optional(targetSub);
        entity.result = required(result, "result");
        if (!("SUCCESS".equals(entity.result)
                || "FAILURE".equals(entity.result))) {
            throw new IllegalArgumentException("invalid audit result");
        }
        entity.reason = required(reason, "reason");
        entity.payload = required(payload, "payload");
        entity.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        return entity;
    }

    public String getResult() {
        return result;
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorSub() {
        return actorSub;
    }

    public String getTargetSub() {
        return targetSub;
    }

    public String getReason() {
        return reason;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
