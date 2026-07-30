package top.egon.cola.platform.rbac3.admin.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rbac3_user_credential")
public class UserCredentialEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 32)
    private CredentialType credentialType;

    @Column(name = "password_hash", length = 512)
    private String passwordHash;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    protected UserCredentialEntity() {
    }

    public UserCredentialEntity(
            Long id,
            Long tenantId,
            Long userId,
            String passwordHash,
            boolean mustChangePassword,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.credentialType = CredentialType.PASSWORD;
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.mustChangePassword = mustChangePassword;
        this.passwordChangedAt = Objects.requireNonNull(now, "now");
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public void recordFailure(int attempts, Instant until, String actorId, Instant now) {
        failedAttempts = attempts;
        lockedUntil = until;
        status = until == null ? Status.ACTIVE : Status.LOCKED;
        markUpdated(actorId, now);
    }

    public void recordSuccess(String actorId, Instant now) {
        failedAttempts = 0;
        lockedUntil = null;
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    public void replacePasswordHash(String nextPasswordHash, String actorId, Instant now) {
        passwordHash = Objects.requireNonNull(nextPasswordHash, "nextPasswordHash");
        credentialVersion = Math.incrementExact(credentialVersion);
        passwordChangedAt = Objects.requireNonNull(now, "now");
        mustChangePassword = false;
        recordSuccess(actorId, now);
    }

    public Long getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Status getStatus() {
        return status;
    }

    public enum CredentialType {
        PASSWORD
    }

    public enum Status {
        ACTIVE,
        LOCKED,
        DISABLED,
        EXPIRED
    }
}
