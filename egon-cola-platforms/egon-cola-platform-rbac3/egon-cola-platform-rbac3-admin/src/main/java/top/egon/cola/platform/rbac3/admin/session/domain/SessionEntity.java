package top.egon.cola.platform.rbac3.admin.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;

@Entity
@Table(name = "rbac3_session")
public class SessionEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "auth_version_at_issue", nullable = false)
    private long authVersionAtIssue;

    @Column(name = "policy_version_at_issue", nullable = false)
    private long policyVersionAtIssue;

    @Column(name = "active_root_checksum", length = 128)
    private String activeRootChecksum;

    @Column(name = "activation_required", nullable = false)
    private boolean activationRequired;

    @Column(name = "token_family_id", nullable = false, length = 128)
    private String tokenFamilyId;

    @Column(name = "device_id_hash", length = 128)
    private String deviceIdHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_strength", nullable = false, length = 32)
    private AuthenticationStrength authenticationStrength;

    @Column(name = "authenticated_at", nullable = false)
    private Instant authenticatedAt;

    @Column(name = "strong_authenticated_at")
    private Instant strongAuthenticatedAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    protected SessionEntity() {
    }

    public SessionEntity(
            Long id,
            Long tenantId,
            Long userId,
            Long sessionId,
            long authVersion,
            long policyVersion,
            String tokenFamilyId,
            String deviceIdHash,
            AuthenticationStrength authenticationStrength,
            Instant authenticatedAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt,
            String actorId) {
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
        if (!idleExpiresAt.isAfter(authenticatedAt)
                || idleExpiresAt.isAfter(absoluteExpiresAt)) {
            throw new IllegalArgumentException("invalid session expiry window");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.status = Status.ACTIVE;
        this.authVersionAtIssue = authVersion;
        this.policyVersionAtIssue = policyVersion;
        this.activationRequired = true;
        this.tokenFamilyId = required(tokenFamilyId, "tokenFamilyId");
        this.deviceIdHash = deviceIdHash;
        this.authenticationStrength = Objects.requireNonNull(
                authenticationStrength, "authenticationStrength");
        this.authenticatedAt = Objects.requireNonNull(authenticatedAt, "authenticatedAt");
        this.lastSeenAt = authenticatedAt;
        this.idleExpiresAt = idleExpiresAt;
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt");
        markCreated(actorId, authenticatedAt);
    }

    public void refresh(long currentPolicyVersion, Instant now, Instant nextIdleExpiry, String actorId) {
        requireActive(now);
        if (currentPolicyVersion < 0) {
            throw new IllegalArgumentException("currentPolicyVersion must not be negative");
        }
        sessionVersion = Math.incrementExact(sessionVersion);
        policyVersionAtIssue = currentPolicyVersion;
        lastSeenAt = now;
        idleExpiresAt = nextIdleExpiry.isAfter(absoluteExpiresAt)
                ? absoluteExpiresAt
                : nextIdleExpiry;
        markUpdated(actorId, now);
    }

    public void activateRoles(
            long currentAuthVersion,
            long currentPolicyVersion,
            String rootChecksum,
            String actorId,
            Instant now
    ) {
        requireActive(now);
        if (currentAuthVersion < 0 || currentPolicyVersion < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
        sessionVersion = Math.incrementExact(sessionVersion);
        authVersionAtIssue = currentAuthVersion;
        policyVersionAtIssue = currentPolicyVersion;
        activeRootChecksum = required(rootChecksum, "rootChecksum");
        activationRequired = false;
        lastSeenAt = now;
        markUpdated(actorId, now);
    }

    public void requireRoleReselection(String actorId, Instant now) {
        requireActive(now);
        sessionVersion = Math.incrementExact(sessionVersion);
        activeRootChecksum = null;
        activationRequired = true;
        markUpdated(actorId, now);
    }

    public boolean logout(String actorId, Instant now) {
        if (status == Status.LOGGED_OUT) {
            return false;
        }
        if (status != Status.ACTIVE) {
            return false;
        }
        status = Status.LOGGED_OUT;
        sessionVersion = Math.incrementExact(sessionVersion);
        revokedAt = now;
        revokeReason = "USER_LOGOUT";
        markUpdated(actorId, now);
        return true;
    }

    public boolean compromise(Instant now, String actorId) {
        if (status != Status.ACTIVE) {
            return false;
        }
        status = Status.COMPROMISED;
        sessionVersion = Math.incrementExact(sessionVersion);
        revokedAt = now;
        revokeReason = "REFRESH_TOKEN_REUSED";
        markUpdated(actorId, now);
        return true;
    }

    public boolean revoke(String reason, String actorId, Instant now) {
        if (status != Status.ACTIVE) {
            return false;
        }
        status = Status.REVOKED;
        sessionVersion = Math.incrementExact(sessionVersion);
        revokedAt = now;
        revokeReason = required(reason, "reason");
        markUpdated(actorId, now);
        return true;
    }

    public void stepUp(String actorId, Instant now) {
        requireActive(now);
        authenticationStrength = AuthenticationStrength.STRONG;
        strongAuthenticatedAt = now;
        lastSeenAt = now;
        markUpdated(actorId, now);
    }

    public boolean isStrongAuthenticationRecent(Instant now, Duration maximumAge) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(maximumAge, "maximumAge");
        return authenticationStrength == AuthenticationStrength.STRONG
                && strongAuthenticatedAt != null
                && strongAuthenticatedAt.plus(maximumAge).isAfter(now);
    }

    public void requireActive(Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("session is not active");
        }
        if (!idleExpiresAt.isAfter(now) || !absoluteExpiresAt.isAfter(now)) {
            throw new IllegalStateException("session has expired");
        }
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Status getStatus() {
        return status;
    }

    public long getSessionVersion() {
        return sessionVersion;
    }

    public long getAuthVersionAtIssue() {
        return authVersionAtIssue;
    }

    public long getPolicyVersionAtIssue() {
        return policyVersionAtIssue;
    }

    public String getActiveRootChecksum() {
        return activeRootChecksum;
    }

    public boolean isActivationRequired() {
        return activationRequired;
    }

    public AuthenticationStrength getAuthenticationStrength() {
        return authenticationStrength;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    public Instant getStrongAuthenticatedAt() {
        return strongAuthenticatedAt;
    }

    public Instant getIdleExpiresAt() {
        return idleExpiresAt;
    }

    public String getTokenFamilyId() {
        return tokenFamilyId;
    }

    public Instant getAbsoluteExpiresAt() {
        return absoluteExpiresAt;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        LOGGED_OUT,
        REVOKED,
        EXPIRED,
        COMPROMISED
    }

    public enum AuthenticationStrength {
        PASSWORD,
        MFA,
        STRONG
    }
}
