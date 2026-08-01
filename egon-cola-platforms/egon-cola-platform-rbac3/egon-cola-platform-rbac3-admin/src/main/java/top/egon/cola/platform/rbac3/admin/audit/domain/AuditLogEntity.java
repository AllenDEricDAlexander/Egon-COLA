package top.egon.cola.platform.rbac3.admin.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Append-only, already-redacted security and authorization audit record.
 */
@Entity
@Table(name = "rbac3_audit_log")
public class AuditLogEntity {

    @Id
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;
    @Column(nullable = false, length = 32)
    private String outcome;
    @Column(nullable = false, length = 32)
    private String severity;
    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;
    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;
    @Column(name = "target_type", length = 128)
    private String targetType;
    @Column(name = "target_id", length = 128)
    private String targetId;
    @Column(name = "management_policy_id")
    private Long managementPolicyId;
    @Column(name = "reason_code", length = 128)
    private String reasonCode;
    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;
    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "client_ip", columnDefinition = "inet")
    private String clientIp;
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> beforeSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> afterSnapshot;
    @Column(name = "payload_checksum", nullable = false, length = 256)
    private String payloadChecksum;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(
            Long id,
            Long tenantId,
            String eventType,
            String outcome,
            String severity,
            String actorType,
            String actorId,
            String targetType,
            String targetId,
            Long managementPolicyId,
            String reasonCode,
            String requestId,
            String traceId,
            String clientIp,
            String userAgent,
            Map<String, Object> beforeSnapshot,
            Map<String, Object> afterSnapshot,
            String payloadChecksum,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.eventType = required(eventType, "eventType");
        this.outcome = required(outcome, "outcome");
        this.severity = required(severity, "severity");
        this.actorType = required(actorType, "actorType");
        this.actorId = required(actorId, "actorId");
        this.targetType = optional(targetType, "targetType");
        this.targetId = optional(targetId, "targetId");
        this.managementPolicyId = managementPolicyId;
        this.reasonCode = optional(reasonCode, "reasonCode");
        this.requestId = required(requestId, "requestId");
        this.traceId = required(traceId, "traceId");
        this.clientIp = optional(clientIp, "clientIp");
        this.userAgent = optional(userAgent, "userAgent");
        this.beforeSnapshot = Map.copyOf(Objects.requireNonNull(
                beforeSnapshot, "beforeSnapshot"));
        this.afterSnapshot = Map.copyOf(Objects.requireNonNull(
                afterSnapshot, "afterSnapshot"));
        this.payloadChecksum = required(payloadChecksum, "payloadChecksum");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getSeverity() {
        return severity;
    }

    public String getActorType() {
        return actorType;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public Long getManagementPolicyId() {
        return managementPolicyId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Map<String, Object> getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public Map<String, Object> getAfterSnapshot() {
        return afterSnapshot;
    }

    public String getPayloadChecksum() {
        return payloadChecksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
