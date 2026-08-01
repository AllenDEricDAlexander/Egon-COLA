package top.egon.cola.platform.rbac3.admin.runtime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;

@Entity
@Table(name = "rbac3_idempotency_record")
public class IdempotencyRecordEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;
    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;
    @Column(name = "operation_code", nullable = false, length = 128)
    private String operationCode;
    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;
    @Column(name = "request_hash", nullable = false, length = 256)
    private String requestHash;
    @Column(name = "resource_type", length = 128)
    private String resourceType;
    @Column(name = "resource_id", length = 128)
    private String resourceId;
    @Column(name = "response_status")
    private Integer responseStatus;
    @Column(name = "response_digest", length = 256)
    private String responseDigest;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecordEntity() {
    }

    public IdempotencyRecordEntity(
            Long id,
            Long tenantId,
            ActorType actorType,
            String actorId,
            String operationCode,
            String keyHash,
            String requestHash,
            Instant expiresAt,
            Instant now
    ) {
        this.id = id;
        setTenantId(tenantId);
        this.actorType = actorType;
        this.actorId = required(actorId);
        this.operationCode = required(operationCode);
        this.keyHash = required(keyHash);
        this.requestHash = required(requestHash);
        this.status = Status.PROCESSING;
        this.expiresAt = expiresAt;
        markCreated(actorId, now);
    }

    public void complete(
            String resourceType,
            String resourceId,
            int responseStatus,
            String responseDigest,
            Instant now
    ) {
        if (responseStatus < 100 || responseStatus > 599) {
            throw new IllegalArgumentException("invalid response status");
        }
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.responseStatus = responseStatus;
        this.responseDigest = responseDigest;
        this.status = Status.COMPLETED;
        markUpdated(actorId, now);
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseDigest() {
        return responseDigest;
    }

    public Status getStatus() {
        return status;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value is required");
        }
        return value.trim();
    }

    public enum ActorType {
        USER,
        SERVICE,
        SYSTEM
    }

    public enum Status {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
