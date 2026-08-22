package top.egon.cola.platform.idp.admin.tenant.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/** IdP-owned tenant membership fact mapped to {@code identity_tenant_membership}. */
@Entity
@Table(name = "identity_tenant_membership")
public class IdentityTenantMembershipEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "identity_sub", nullable = false, length = 64)
    private String identitySub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, length = 128,
            updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    protected IdentityTenantMembershipEntity() {
    }

    /** Creates a membership fact at version zero. */
    public static IdentityTenantMembershipEntity create(
            String id,
            String tenantId,
            String identitySub,
            Status status,
            String actor,
            Instant now
    ) {
        IdentityTenantMembershipEntity entity =
                new IdentityTenantMembershipEntity();
        entity.id = required(id, "id");
        entity.tenantId = required(tenantId, "tenantId");
        entity.identitySub = required(identitySub, "identitySub");
        entity.status = Objects.requireNonNull(status, "status");
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        entity.createdBy = required(actor, "actor");
        entity.updatedBy = entity.createdBy;
        return entity;
    }

    /** Applies a version-checked ACTIVE/DISABLED replacement. */
    public void update(
            Status newStatus,
            long expectedVersion,
            String actor,
            Instant now
    ) {
        if (expectedVersion != version) {
            throw new IllegalStateException("membership version conflict");
        }
        status = Objects.requireNonNull(newStatus, "status");
        version = Math.addExact(version, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
        updatedBy = required(actor, "actor");
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getIdentitySub() {
        return identitySub;
    }

    public Status getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /** Membership lifecycle state. */
    public enum Status {
        ACTIVE,
        DISABLED
    }
}
