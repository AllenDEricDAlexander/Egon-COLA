package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "gateway_audit_log")
public class GatewayAuditLogEntity {

    @Id
    private String id;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(nullable = false)
    private String source;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_summary", columnDefinition = "jsonb")
    private Map<String, Object> beforeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_summary", columnDefinition = "jsonb")
    private Map<String, Object> afterSummary;

    @Column(name = "draft_revision")
    private Long draftRevision;

    @Column(name = "release_id")
    private String releaseId;

    @Column(nullable = false)
    private boolean successful;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected GatewayAuditLogEntity() {
    }

    public GatewayAuditLogEntity(
            String id,
            String actorId,
            String actorType,
            String source,
            String requestId,
            String traceId,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary,
            Long draftRevision,
            String releaseId,
            boolean successful,
            String errorCode,
            Instant occurredAt) {
        this.id = id;
        this.actorId = actorId;
        this.actorType = actorType;
        this.source = source;
        this.requestId = requestId;
        this.traceId = traceId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
        this.beforeSummary = sanitized(beforeSummary);
        this.afterSummary = sanitized(afterSummary);
        this.draftRevision = draftRevision;
        this.releaseId = releaseId;
        this.successful = successful;
        this.errorCode = errorCode;
        this.occurredAt = occurredAt;
    }

    private Map<String, Object> sanitized(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> {
            String lower = key.toLowerCase(java.util.Locale.ROOT);
            if (!lower.contains("secret")
                    && !lower.contains("token")
                    && !lower.contains("authorization")
                    && !lower.contains("cookie")) {
                result.put(key, value);
            }
        });
        return Map.copyOf(result);
    }
}
