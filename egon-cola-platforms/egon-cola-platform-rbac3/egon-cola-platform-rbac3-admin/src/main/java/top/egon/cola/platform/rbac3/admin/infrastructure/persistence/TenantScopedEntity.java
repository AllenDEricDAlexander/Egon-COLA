package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.Instant;

@MappedSuperclass
public abstract class TenantScopedEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    public Long getTenantId() {
        return tenantId;
    }

    protected void setTenantId(Long tenantId) {
        if (this.tenantId != null && !this.tenantId.equals(tenantId)) {
            throw new IllegalStateException("tenantId is immutable");
        }
        this.tenantId = tenantId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void markCreated(String actorId, Instant now) {
        if (createdAt != null) {
            throw new IllegalStateException("creation audit is immutable");
        }
        createdAt = now;
        createdBy = requiredActor(actorId);
        updatedAt = now;
        updatedBy = createdBy;
    }

    public void markUpdated(String actorId, Instant now) {
        updatedAt = now;
        updatedBy = requiredActor(actorId);
    }

    @PrePersist
    void requireCreationAudit() {
        if (createdAt == null || createdBy == null || updatedAt == null || updatedBy == null) {
            throw new IllegalStateException("creation audit must be initialized before persistence");
        }
    }

    @PreUpdate
    void requireUpdateAudit() {
        if (updatedAt == null || updatedBy == null) {
            throw new IllegalStateException("update audit must be initialized before persistence");
        }
    }

    private static String requiredActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required");
        }
        return actorId.trim();
    }
}
