package top.egon.cola.platform.idp.admin.identity.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_user")
public class IdentityUserEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "username_normalized", nullable = false, length = 128)
    private String normalizedUsername;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdentityUserStatus status;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityUserEntity() {
    }

    public static IdentityUserEntity fromDomain(
            IdentityUser user,
            Instant now
    ) {
        IdentityUserEntity entity = new IdentityUserEntity();
        entity.id = user.id();
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.apply(user, now);
        return entity;
    }

    public void apply(IdentityUser user, Instant now) {
        Objects.requireNonNull(user, "user");
        if (id != null && !id.equals(user.id())) {
            throw new IllegalArgumentException("identity id cannot change");
        }
        id = user.id();
        username = user.username();
        normalizedUsername = user.normalizedUsername();
        displayName = user.displayName();
        status = user.status();
        failedLoginCount = user.failedLoginCount();
        lockedUntil = user.lockedUntil();
        lastLoginAt = user.lastLoginAt();
        version = user.version();
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public IdentityUser toDomain() {
        return new IdentityUser(
                id,
                username,
                normalizedUsername,
                displayName,
                status,
                failedLoginCount,
                lockedUntil,
                lastLoginAt,
                version
        );
    }

    public String getId() {
        return id;
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
}
