package top.egon.cola.platform.rbac3.admin.session.domain;

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
@Table(name = "rbac3_refresh_token")
public class RefreshTokenEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "family_id", nullable = false, length = 128)
    private String familyId;

    @Column(nullable = false)
    private long generation;

    @Column(name = "token_hash", nullable = false, length = 256, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "reuse_detected_at")
    private Instant reuseDetectedAt;

    protected RefreshTokenEntity() {
    }

    public RefreshTokenEntity(
            Long id,
            Long tenantId,
            Long sessionId,
            String familyId,
            long generation,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            String actorId) {
        if (generation < 0) {
            throw new IllegalArgumentException("generation must not be negative");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.familyId = required(familyId, "familyId");
        this.generation = generation;
        this.tokenHash = required(tokenHash, "tokenHash");
        this.status = Status.ACTIVE;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        markCreated(actorId, issuedAt);
    }

    public void rotate(Long nextTokenId, Instant now, String actorId) {
        requireActive();
        status = Status.ROTATED;
        rotatedAt = now;
        replacedById = Objects.requireNonNull(nextTokenId, "nextTokenId");
        markUpdated(actorId, now);
    }

    public void markReused(Instant now, String actorId) {
        if (status != Status.ROTATED && status != Status.REUSED_DETECTED) {
            throw new IllegalStateException("only rotated token can be marked reused");
        }
        status = Status.REUSED_DETECTED;
        reuseDetectedAt = now;
        markUpdated(actorId, now);
    }

    public void revoke(Instant now, String actorId) {
        if (status == Status.ACTIVE) {
            status = Status.REVOKED;
            markUpdated(actorId, now);
        }
    }

    public void requireActive() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("refresh token is not active");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public long getGeneration() {
        return generation;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        ROTATED,
        REUSED_DETECTED,
        REVOKED,
        EXPIRED
    }
}
