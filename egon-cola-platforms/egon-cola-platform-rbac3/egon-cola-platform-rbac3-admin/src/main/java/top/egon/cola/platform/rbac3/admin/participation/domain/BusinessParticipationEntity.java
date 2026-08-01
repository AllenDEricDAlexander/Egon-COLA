package top.egon.cola.platform.rbac3.admin.participation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Append-only evidence that a user performed an action on one business object.
 */
@Entity
@Table(name = "rbac3_business_participation")
public class BusinessParticipationEntity {

    @Id
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;
    @Column(name = "business_resource", nullable = false, length = 128)
    private String businessResource;
    @Column(name = "business_id", nullable = false, length = 256)
    private String businessId;
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    @Column(name = "action_code", nullable = false, length = 128)
    private String actionCode;
    @Column(name = "business_event_id", nullable = false, length = 128)
    private String businessEventId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;
    @Column(name = "payload_digest", nullable = false, length = 128)
    private String payloadDigest;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    protected BusinessParticipationEntity() {
    }

    public BusinessParticipationEntity(
            Long id,
            Long tenantId,
            String applicationCode,
            String businessResource,
            String businessId,
            Long actorUserId,
            String actionCode,
            String businessEventId,
            Instant occurredAt,
            String traceId,
            String payloadDigest,
            Instant createdAt,
            String createdBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.applicationCode = required(applicationCode, "applicationCode");
        this.businessResource = required(businessResource, "businessResource");
        this.businessId = required(businessId, "businessId");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        this.actionCode = required(actionCode, "actionCode");
        this.businessEventId = required(businessEventId, "businessEventId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.traceId = required(traceId, "traceId");
        this.payloadDigest = required(payloadDigest, "payloadDigest");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.createdBy = required(createdBy, "createdBy");
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getBusinessResource() {
        return businessResource;
    }

    public String getBusinessId() {
        return businessId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getBusinessEventId() {
        return businessEventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPayloadDigest() {
        return payloadDigest;
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
}
