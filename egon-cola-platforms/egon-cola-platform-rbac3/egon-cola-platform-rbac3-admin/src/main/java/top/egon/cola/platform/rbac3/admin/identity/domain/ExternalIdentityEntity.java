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
@Table(name = "rbac3_external_identity")
public class ExternalIdentityEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "provider_code", nullable = false, length = 128)
    private String providerCode;

    @Column(name = "external_subject_id", nullable = false, length = 256)
    private String externalSubjectId;

    @Column(name = "identity_sub", nullable = false, length = 512)
    private String identitySub;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "sync_version", nullable = false)
    private long syncVersion;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected ExternalIdentityEntity() {
    }

    public ExternalIdentityEntity(
            Long id,
            Long tenantId,
            String providerCode,
            String externalSubjectId,
            Long userId,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.providerCode = required(providerCode, "providerCode");
        this.externalSubjectId = required(externalSubjectId, "externalSubjectId");
        this.identitySub = this.externalSubjectId;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = Status.ACTIVE;
        this.lastSyncedAt = Objects.requireNonNull(now, "now");
        markCreated(actorId, now);
    }

    public static ExternalIdentityEntity idpMapping(
            Long id,
            Long tenantId,
            String identitySub,
            Long userId,
            String actorId,
            Instant now) {
        return new ExternalIdentityEntity(
                id, tenantId, "IDP", identitySub, userId, actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getIdentitySub() {
        return identitySub;
    }

    public Long getUserId() {
        return userId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        STALE
    }
}
