package top.egon.cola.platform.idp.admin.identity.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_user_credential")
public class IdentityCredentialEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "identity_sub", nullable = false, length = 64)
    private String identitySub;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 32)
    private CredentialType credentialType;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PasswordCredential.Status status;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityCredentialEntity() {
    }

    public static IdentityCredentialEntity fromDomain(
            PasswordCredential credential,
            Instant now
    ) {
        IdentityCredentialEntity entity = new IdentityCredentialEntity();
        entity.id = credential.identitySub();
        entity.identitySub = credential.identitySub();
        entity.credentialType = CredentialType.PASSWORD;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.apply(credential, now);
        return entity;
    }

    public void apply(PasswordCredential credential, Instant now) {
        Objects.requireNonNull(credential, "credential");
        if (identitySub != null
                && !identitySub.equals(credential.identitySub())) {
            throw new IllegalArgumentException(
                    "credential identity cannot change"
            );
        }
        identitySub = credential.identitySub();
        if (id == null) {
            id = credential.identitySub();
        }
        credentialType = CredentialType.PASSWORD;
        passwordHash = credential.passwordHash();
        passwordChangedAt = credential.passwordChangedAt();
        mustChangePassword = credential.mustChangePassword();
        status = credential.status();
        version = credential.version();
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public PasswordCredential toDomain() {
        return new PasswordCredential(
                identitySub,
                passwordHash,
                passwordChangedAt,
                mustChangePassword,
                status,
                version
        );
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public enum CredentialType {
        PASSWORD
    }
}
