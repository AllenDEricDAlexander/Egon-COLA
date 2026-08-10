package top.egon.cola.platform.idp.admin.token.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_signing_key")
public class IdentitySigningKeyEntity {

    @Id
    @Column(length = 128)
    private String kid;

    @Column(nullable = false, length = 32)
    private String algorithm;

    @Column(name = "encrypted_private_key", nullable = false)
    private String encryptedPrivateKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "public_jwk", nullable = false, columnDefinition = "jsonb")
    private String publicJwk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentitySigningKeyEntity() {
    }

    public static IdentitySigningKeyEntity published(
            String kid,
            String encryptedPrivateKey,
            String publicJwk,
            Instant now
    ) {
        IdentitySigningKeyEntity entity = new IdentitySigningKeyEntity();
        entity.kid = required(kid, "kid");
        entity.algorithm = "RS256";
        entity.encryptedPrivateKey = required(
                encryptedPrivateKey,
                "encryptedPrivateKey"
        );
        entity.publicJwk = required(publicJwk, "publicJwk");
        entity.status = Status.PUBLISHED;
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        return entity;
    }

    public String getKid() {
        return kid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Status getStatus() {
        return status;
    }

    public String getPublicJwk() {
        return publicJwk;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getRetiredAt() {
        return retiredAt;
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

    public void activate(long expectedVersion, Instant now) {
        if (version != expectedVersion) {
            throw new IllegalStateException("stale signing key version");
        }
        if (status == Status.RETIRED) {
            throw new IllegalStateException("retired signing key cannot activate");
        }
        status = Status.ACTIVE;
        activatedAt = Objects.requireNonNull(now, "now");
        retiredAt = null;
        updatedAt = now;
        version = Math.addExact(version, 1L);
    }

    public void retire(long expectedVersion, Instant now) {
        if (version != expectedVersion) {
            throw new IllegalStateException("stale signing key version");
        }
        if (status == Status.RETIRED) {
            return;
        }
        status = Status.RETIRED;
        retiredAt = Objects.requireNonNull(now, "now");
        updatedAt = now;
        version = Math.addExact(version, 1L);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    public enum Status {
        PUBLISHED,
        ACTIVE,
        RETIRED
    }
}
