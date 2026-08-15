package top.egon.cola.platform.rbac3.admin.iam.user.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;

/**
 * RBAC's local user projection. Credentials and profile data belong to IdP;
 * this entity only binds an IdP subject to a tenant and tracks authorization
 * invalidation state.
 */
@Entity(name = "UserEntity")
@Table(name = "rbac3_user")
public class UserPO extends TenantScopedPO {

    @Id
    private Long id;

    @Column(name = "identity_sub", nullable = false, length = 200)
    private String identitySub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatusEnum status;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    protected UserPO() {
    }

    public UserPO(
            Long id,
            Long tenantId,
            String identitySub,
            UserStatusEnum status,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.identitySub = required(identitySub, "identitySub");
        this.status = Objects.requireNonNull(status, "status");
        markCreated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getIdentitySub() {
        return identitySub;
    }

    public UserStatusEnum getStatus() {
        return status;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public void changeStatus(
            UserStatusEnum nextStatus,
            String reason,
            String actorId,
            Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        required(reason, "reason");
        if (status == UserStatusEnum.ARCHIVED) {
            throw new IllegalStateException("archived user is terminal");
        }
        status = nextStatus;
        authVersion = Math.incrementExact(authVersion);
        markUpdated(actorId, now);
    }

    public void changeStatus(
            UserStatusEnum nextStatus,
            String reason,
            long expectedAuthVersion,
            String actorId,
            Instant now) {
        if (authVersion != expectedAuthVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        changeStatus(nextStatus, reason, actorId, now);
    }

    /**
     * Applies an external directory change without copying directory profile
     * fields into RBAC. Only authorization-relevant changes advance the local
     * version used by authorization snapshots.
     */
    public void applyAuthorizationChange(
            boolean authorizationChanged,
            String actorId,
            Instant now) {
        if (authorizationChanged) {
            incrementAuthVersion(actorId, now);
        } else {
            markUpdated(actorId, now);
        }
    }

    public long advanceAuthorizationVersion(
            long expectedVersion,
            String actorId,
            Instant now) {
        if (authVersion != expectedVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        return incrementAuthVersion(actorId, now);
    }

    public long incrementAuthVersion(String actorId, Instant now) {
        authVersion = Math.incrementExact(authVersion);
        markUpdated(actorId, now);
        return authVersion;
    }

    public void updateIdentitySub(
            String nextIdentitySub,
            long expectedAuthVersion,
            String actorId,
            Instant now) {
        if (authVersion != expectedAuthVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        identitySub = required(nextIdentitySub, "identitySub");
        incrementAuthVersion(actorId, now);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
