package top.egon.cola.platform.rbac3.admin.iam.authorizationstate.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

/** RBAC-owned tenant authorization state after IdP catalog externalization. */
@Entity(name = "TenantAuthorizationStateEntity")
@Table(name = "rbac3_tenant_authorization_state")
public class TenantAuthorizationStatePO {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "policy_version", nullable = false)
    private long policyVersion;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    protected TenantAuthorizationStatePO() {
    }

    public TenantAuthorizationStatePO(
            Long tenantId,
            String actorId,
            Instant now
    ) {
        this.tenantId = requireTenant(tenantId);
        this.policyVersion = 0L;
        this.version = 0L;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.createdBy = required(actorId, "actorId");
        this.updatedAt = this.createdAt;
        this.updatedBy = this.createdBy;
    }

    /** Increments the authorization policy version while preserving audit state. */
    public void incrementPolicyVersion(String actorId, Instant now) {
        policyVersion = Math.incrementExact(policyVersion);
        updatedAt = Objects.requireNonNull(now, "now");
        updatedBy = required(actorId, "actorId");
    }

    public Long getTenantId() {
        return tenantId;
    }

    public long getPolicyVersion() {
        return policyVersion;
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

    private static Long requireTenant(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("tenantId is invalid");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
