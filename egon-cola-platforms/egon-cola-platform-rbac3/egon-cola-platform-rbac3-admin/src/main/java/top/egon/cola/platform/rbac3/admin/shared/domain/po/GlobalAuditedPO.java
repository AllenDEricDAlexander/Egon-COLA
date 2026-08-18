package top.egon.cola.platform.rbac3.admin.shared.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.Instant;

/** Common audit/version state for RBAC catalog rows that are not tenant-scoped. */
@MappedSuperclass
public abstract class GlobalAuditedPO {

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
        if (createdAt == null || createdBy == null
                || updatedAt == null || updatedBy == null) {
            throw new IllegalStateException("creation audit is required");
        }
    }

    @PreUpdate
    void requireUpdateAudit() {
        if (updatedAt == null || updatedBy == null) {
            throw new IllegalStateException("update audit is required");
        }
    }

    private static String requiredActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required");
        }
        return actorId.trim();
    }
}
