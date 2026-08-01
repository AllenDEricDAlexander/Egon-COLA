package top.egon.cola.platform.rbac3.admin.auth.domain;

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
@Table(name = "rbac3_service_credential")
public class ServiceCredentialEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "principal_id", nullable = false)
    private Long principalId;

    @Column(name = "credential_id", nullable = false, length = 128)
    private String credentialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 32)
    private CredentialType credentialType;

    @Column(name = "secret_hash", length = 512)
    private String secretHash;

    @Column(name = "public_key", columnDefinition = "text")
    private String publicKey;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected ServiceCredentialEntity() {
    }

    public ServiceCredentialEntity(
            Long id,
            Long tenantId,
            Long principalId,
            String credentialId,
            CredentialType credentialType,
            String secretHash,
            String publicKey,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        boolean validClientSecret = credentialType == CredentialType.CLIENT_SECRET
                && secretHash != null && publicKey == null;
        boolean validPublicKey = credentialType == CredentialType.PUBLIC_KEY
                && secretHash == null && publicKey != null;
        if (!validClientSecret && !validPublicKey) {
            throw new IllegalArgumentException("credential material does not match credential type");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.principalId = Objects.requireNonNull(principalId, "principalId");
        this.credentialId = required(credentialId, "credentialId");
        this.credentialType = Objects.requireNonNull(credentialType, "credentialType");
        this.secretHash = secretHash;
        this.publicKey = publicKey;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum CredentialType {
        CLIENT_SECRET,
        PUBLIC_KEY
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED,
        REVOKED
    }
}
